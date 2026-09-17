package com.github.yannicklamprecht.mc.management.spring;

import com.github.yannicklamprecht.mc.management.api.DefaultMinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * With several servers configured, one being offline or misconfigured must not take down access
 * to the others - or fail application startup entirely, since {@link MinecraftManagementClientLifecycle}
 * is a {@link org.springframework.context.SmartLifecycle} bean and an exception from its
 * {@code start()} aborts the whole Spring application context.
 */
class MinecraftManagementClientLifecycleTests {

    @Test
    void oneServerFailingToConnectDoesNotPreventOthersOrFailStartup() {
        MinecraftManagementServerRegistry.ManagedServer healthy = managedServer("healthy",
                CompletableFuture.completedFuture(null));
        MinecraftManagementServerRegistry.ManagedServer broken = managedServer("broken",
                CompletableFuture.failedFuture(new ConnectException("Connection refused")));

        Map<String, MinecraftManagementServerRegistry.ManagedServer> servers = new LinkedHashMap<>();
        servers.put("healthy", healthy);
        servers.put("broken", broken);
        MinecraftManagementServerRegistry registry = new MinecraftManagementServerRegistry(servers);

        MinecraftManagementClientLifecycle lifecycle = new MinecraftManagementClientLifecycle(registry);

        assertThatCode(lifecycle::start).doesNotThrowAnyException();
        assertThat(lifecycle.isRunning()).isTrue();

        verify(healthy.eventBridge()).registerListeners();
        verify(broken.eventBridge()).registerListeners();
        verify(healthy.client()).connect();
        verify(broken.client()).connect();
    }

    @Test
    void protocolVersionIsAutoDetectedAfterConnectingWhenNotConfigured() {
        MinecraftManagementClient client = mock(MinecraftManagementClient.class);
        when(client.connect()).thenReturn(CompletableFuture.completedFuture(null));
        when(client.detectProtocolVersion()).thenReturn(CompletableFuture.completedFuture("1.0.0"));
        DefaultMinecraftManagementSession session = new DefaultMinecraftManagementSession(client, null);
        MinecraftManagementEventBridge eventBridge = mock(MinecraftManagementEventBridge.class);
        MinecraftManagementProperties.ServerProperties properties = new MinecraftManagementProperties.ServerProperties();
        // protocol-version intentionally left unset (null) - auto-detect mode.

        Map<String, MinecraftManagementServerRegistry.ManagedServer> servers = new LinkedHashMap<>();
        servers.put("auto", new MinecraftManagementServerRegistry.ManagedServer("auto", client, session, eventBridge, properties));
        MinecraftManagementServerRegistry registry = new MinecraftManagementServerRegistry(servers);

        new MinecraftManagementClientLifecycle(registry).start();

        verify(client).detectProtocolVersion();
        assertThat(session.protocolVersion()).isEqualTo("1.0.0");
    }

    @Test
    void aFailedDetectionLeavesTheServerOnItsPlaceholderProtocolVersionInsteadOfFailingStartup() {
        MinecraftManagementClient client = mock(MinecraftManagementClient.class);
        when(client.connect()).thenReturn(CompletableFuture.completedFuture(null));
        when(client.detectProtocolVersion()).thenReturn(CompletableFuture.failedFuture(new ConnectException("Connection refused")));
        DefaultMinecraftManagementSession session = new DefaultMinecraftManagementSession(client, null);
        String placeholderProtocolVersion = session.protocolVersion();
        MinecraftManagementEventBridge eventBridge = mock(MinecraftManagementEventBridge.class);
        MinecraftManagementProperties.ServerProperties properties = new MinecraftManagementProperties.ServerProperties();

        Map<String, MinecraftManagementServerRegistry.ManagedServer> servers = new LinkedHashMap<>();
        servers.put("auto", new MinecraftManagementServerRegistry.ManagedServer("auto", client, session, eventBridge, properties));
        MinecraftManagementServerRegistry registry = new MinecraftManagementServerRegistry(servers);
        MinecraftManagementClientLifecycle lifecycle = new MinecraftManagementClientLifecycle(registry);

        assertThatCode(lifecycle::start).doesNotThrowAnyException();
        assertThat(lifecycle.isRunning()).isTrue();
        assertThat(session.protocolVersion()).isEqualTo(placeholderProtocolVersion);
    }

    private static MinecraftManagementServerRegistry.ManagedServer managedServer(String id, CompletableFuture<Void> connectResult) {
        MinecraftManagementClient client = mock(MinecraftManagementClient.class);
        when(client.connect()).thenReturn(connectResult);
        MinecraftManagementSession session = mock(MinecraftManagementSession.class);
        MinecraftManagementEventBridge eventBridge = mock(MinecraftManagementEventBridge.class);
        MinecraftManagementProperties.ServerProperties properties = new MinecraftManagementProperties.ServerProperties();
        // Pin an explicit protocol version so this helper's tests exercise connect-failure
        // resilience only, independent of the auto-detection behavior covered above.
        properties.setProtocolVersion("3.1.0");
        return new MinecraftManagementServerRegistry.ManagedServer(id, client, session, eventBridge, properties);
    }
}
