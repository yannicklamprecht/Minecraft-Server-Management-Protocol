package com.example.msmp;

import com.example.msmp.api.MinecraftManagementSession;
import com.example.msmp.transport.MinecraftManagementClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VersionAgnosticSessionTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testVersionAgnosticNotificationsAcrossVersions() throws Exception {
        MinecraftManagementClient client = new MinecraftManagementClient(mapper, URI.create("ws://localhost:25585"), "test");
        MinecraftManagementSession session = client.session();

        AtomicReference<MinecraftManagementSession.PlayerView> joined = new AtomicReference<>();
        AtomicReference<MinecraftManagementSession.ServerStatusView> statusRef = new AtomicReference<>();
        AtomicBoolean started = new AtomicBoolean(false);

        session.onPlayerJoined(joined::set);
        session.onServerStatus(statusRef::set);
        session.onServerStarted(() -> started.set(true));

        Method processMessage = MinecraftManagementClient.class.getDeclaredMethod("processMessage", String.class);
        processMessage.setAccessible(true);

        // Test 1: Player joined notification
        String playerJoinedJson = "{\"jsonrpc\":\"2.0\",\"method\":\"minecraft:notification/players/joined\",\"params\":{\"player\":{\"id\":\"p-1\",\"name\":\"Steve\"}}}";
        processMessage.invoke(client, playerJoinedJson);

        assertNotNull(joined.get());
        assertEquals("p-1", joined.get().id());
        assertEquals("Steve", joined.get().name());

        // Test 2: Server status notification
        String statusJson = "{\"jsonrpc\":\"2.0\",\"method\":\"minecraft:notification/server/status\",\"params\":{\"started\":true,\"players\":[{\"id\":\"p-1\",\"name\":\"Steve\"}],\"version\":{\"name\":\"1.21.4\",\"protocol\":768}}}";
        processMessage.invoke(client, statusJson);

        assertNotNull(statusRef.get());
        assertTrue(statusRef.get().started());
        assertEquals(1, statusRef.get().players().size());
        assertEquals("Steve", statusRef.get().players().get(0).name());
        assertEquals("1.21.4", statusRef.get().version().name());
        assertEquals(768L, statusRef.get().version().protocol());

        // Test 3: Server started notification
        String startedJson = "{\"jsonrpc\":\"2.0\",\"method\":\"minecraft:notification/server/started\",\"params\":{}}";
        processMessage.invoke(client, startedJson);

        assertTrue(started.get());
    }

    @Test
    void testAllGeneratedVersionsDirectFacades() {
        MinecraftManagementClient client = new MinecraftManagementClient(mapper, URI.create("ws://localhost:25585"), "test");

        assertNotNull(client.v1_0_0());
        assertNotNull(client.notificationsV1_0_0());

        assertNotNull(client.v2_0_0());
        assertNotNull(client.notificationsV2_0_0());

        assertNotNull(client.v3_0_0());
        assertNotNull(client.notificationsV3_0_0());

        assertNotNull(client.v3_1_0());
        assertNotNull(client.notificationsV3_1_0());

        assertNotNull(client.session());
        assertEquals("3.1.0", client.session().protocolVersion());
        assertEquals("1.0.0", client.session("1.0.0").protocolVersion());
    }
}
