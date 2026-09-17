package com.github.yannicklamprecht.mc.management.spring;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * Holds one {@link MinecraftManagementClient}/{@link MinecraftManagementSession} pair per server
 * configured under {@code minecraft.management.servers.*} (or the legacy flat properties, folded
 * in under the id {@code "default"} - see {@link MinecraftManagementProperties}).
 * <p>
 * This is the entry point applications should depend on instead of a single
 * {@code MinecraftManagementSession} bean, since there is no longer exactly one.
 */
public final class MinecraftManagementServerRegistry {

    /** One configured, wired-up MSMP server: its client, session, and notification-to-event bridge. */
    public record ManagedServer(
            String id,
            MinecraftManagementClient client,
            MinecraftManagementSession session,
            MinecraftManagementEventBridge eventBridge,
            MinecraftManagementProperties.ServerProperties properties) {
    }

    private final Map<String, ManagedServer> servers;

    public MinecraftManagementServerRegistry(Map<String, ManagedServer> servers) {
        this.servers = Map.copyOf(servers);
    }

    /** The ids of every configured server, in configuration order. */
    public Set<String> serverIds() {
        return servers.keySet();
    }

    public Collection<ManagedServer> allServers() {
        return servers.values();
    }

    public Optional<ManagedServer> find(String id) {
        return Optional.ofNullable(servers.get(id));
    }

    public ManagedServer get(String id) {
        ManagedServer server = servers.get(id);
        if (server == null) {
            throw new NoSuchElementException("No MSMP server configured with id '" + id
                    + "' (configured: " + servers.keySet() + ")");
        }
        return server;
    }

    /** Convenience accessor for {@code get(id).session()}. */
    public MinecraftManagementSession session(String id) {
        return get(id).session();
    }

    public boolean isEmpty() {
        return servers.isEmpty();
    }
}
