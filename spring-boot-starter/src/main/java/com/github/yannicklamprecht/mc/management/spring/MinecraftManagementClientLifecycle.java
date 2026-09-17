package com.github.yannicklamprecht.mc.management.spring;

import com.github.yannicklamprecht.mc.management.api.DefaultMinecraftManagementSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Connects every configured MSMP server when the application context starts and keeps them
 * running for the lifetime of the context, honoring each server's own
 * {@code minecraft.management.servers.<id>.auto-connect} setting individually. Notification
 * listeners are registered before each connection is opened so no notification is missed once the
 * handshake completes.
 * <p>
 * For a server with no explicit {@code protocol-version} configured, once its connection succeeds
 * this also detects the actual protocol version (see {@code MinecraftManagementClient#detectProtocolVersion()})
 * and applies it to that server's session, correcting the placeholder version it was built with.
 * A failure to detect it is logged and left at that placeholder, the same way a connection failure
 * leaves a server registered but non-functional - see below.
 * <p>
 * A connection failure for one server is logged and does not stop the others from connecting, nor
 * does it fail application startup: with several servers configured, one being offline or
 * misconfigured shouldn't take down access to the rest. Calls against a server that never
 * connected simply fail with "Not connected" (see {@code MinecraftManagementClient.call}) until it
 * reconnects on a future restart.
 * <p>
 * The connections themselves do not block the JVM from exiting; they simply live as long as the
 * surrounding Spring application does (e.g. as long as an embedded web server is serving requests).
 */
public class MinecraftManagementClientLifecycle implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftManagementClientLifecycle.class);

    private final MinecraftManagementServerRegistry registry;
    private volatile boolean running;

    public MinecraftManagementClientLifecycle(MinecraftManagementServerRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void start() {
        for (MinecraftManagementServerRegistry.ManagedServer server : registry.allServers()) {
            server.eventBridge().registerListeners();
        }
        for (MinecraftManagementServerRegistry.ManagedServer server : registry.allServers()) {
            if (!server.properties().isAutoConnect()) {
                LOGGER.debug("Skipping auto-connect for MSMP server '{}' (auto-connect disabled)", server.id());
                continue;
            }
            try {
                connectAndDetect(server).join();
            } catch (RuntimeException e) {
                LOGGER.error("Failed to connect to MSMP server '{}' at {} ({}) - it will be unavailable until the application is restarted with a reachable configuration",
                        server.id(), server.properties().getUrl(), describe(e));
            }
        }
        running = true;
    }

    /**
     * Connects a single configured server on demand - e.g. a manual "retry" action from the console
     * UI - regardless of its {@code auto-connect} setting, since this is an explicit user request
     * rather than the automatic startup pass. A no-op, already-successful future if it's already
     * connected. Unlike {@link #start()}, a connection failure is not logged here and instead left
     * for the caller to handle (e.g. reporting it back to whoever asked for the retry).
     */
    public CompletableFuture<Void> reconnect(String serverId) {
        MinecraftManagementServerRegistry.ManagedServer server = registry.get(serverId);
        if (server.client().isConnected()) {
            return CompletableFuture.completedFuture(null);
        }
        return connectAndDetect(server);
    }

    private CompletableFuture<Void> connectAndDetect(MinecraftManagementServerRegistry.ManagedServer server) {
        LOGGER.info("Connecting to MSMP server '{}' at {}", server.id(), server.properties().getUrl());
        return server.client().connect().thenRun(() -> {
            LOGGER.info("Connected to MSMP server '{}'", server.id());
            if (server.properties().isProtocolVersionAutoDetected()) {
                detectProtocolVersion(server);
            }
        });
    }

    private void detectProtocolVersion(MinecraftManagementServerRegistry.ManagedServer server) {
        try {
            String detected = server.client().detectProtocolVersion().join();
            if (server.session() instanceof DefaultMinecraftManagementSession session) {
                session.updateProtocolVersion(detected);
            }
            LOGGER.info("Detected MSMP protocol version {} for server '{}'", detected, server.id());
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to auto-detect the MSMP protocol version for server '{}' ({}); continuing with {} until " +
                            "the application is restarted - set minecraft.management.servers.{}.protocol-version explicitly to avoid this",
                    server.id(), describe(e), server.session().protocolVersion(), server.id());
        }
    }

    /**
     * A one-line description of a connection failure (e.g. "ConnectException: Connection refused")
     * instead of the full stack trace - these are expected, common occurrences (the Minecraft
     * server is offline, unreachable, or misconfigured), not bugs to be dumped as a stack trace on
     * every application startup.
     */
    private static String describe(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return cause.getClass().getSimpleName() + (message != null && !message.isBlank() ? ": " + message : "");
    }

    @Override
    public void stop() {
        running = false;
        for (MinecraftManagementServerRegistry.ManagedServer server : registry.allServers()) {
            server.client().close();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        // Start after regular application beans, stop before them.
        return Integer.MAX_VALUE;
    }
}
