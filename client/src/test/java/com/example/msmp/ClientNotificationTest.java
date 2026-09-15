package com.example.msmp;


import com.example.msmp.transport.MinecraftManagementClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.craftstuebchen.mc.management.v3_1_0.MinecraftManagementNotifications;
import de.craftstuebchen.mc.management.v3_1_0.dto.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ClientNotificationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testNotificationPropertyUnwrapping() throws Exception {
        MinecraftManagementClient client = new MinecraftManagementClient(mapper, URI.create("ws://localhost:25585"), "test");
        MinecraftManagementNotifications notifications = client.notificationsV3_1_0();

        AtomicReference<Player> joinedPlayer = new AtomicReference<>();
        notifications.onPlayersJoined(joinedPlayer::set);

        // Simulate incoming WebSocket text message for player joined
        String incomingJson = "{\"jsonrpc\":\"2.0\",\"method\":\"minecraft:notification/players/joined\",\"params\":{\"player\":{\"id\":\"p-123\",\"name\":\"Notch\"}}}";

        Method processMessageMethod = MinecraftManagementClient.class.getDeclaredMethod("processMessage", String.class);
        processMessageMethod.setAccessible(true);
        processMessageMethod.invoke(client, incomingJson);

        assertNotNull(joinedPlayer.get());
        assertEquals("Notch", joinedPlayer.get().name());
        assertEquals("p-123", joinedPlayer.get().id());
    }

    @Test
    void testNoParamNotification() throws Exception {
        MinecraftManagementClient client = new MinecraftManagementClient(mapper, URI.create("ws://localhost:25585"), "test");
        MinecraftManagementNotifications notifications = client.notificationsV3_1_0();

        AtomicBoolean started = new AtomicBoolean(false);
        notifications.onServerStarted(() -> started.set(true));

        String incomingJson = "{\"jsonrpc\":\"2.0\",\"method\":\"minecraft:notification/server/started\",\"params\":{}}";

        Method processMessageMethod = MinecraftManagementClient.class.getDeclaredMethod("processMessage", String.class);
        processMessageMethod.setAccessible(true);
        processMessageMethod.invoke(client, incomingJson);

        assertTrue(started.get());
    }
}
