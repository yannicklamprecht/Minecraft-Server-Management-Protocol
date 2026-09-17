package com.github.yannicklamprecht.mc.management;


import com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yannicklamprecht.mc.management.v3_1_0.MinecraftManagementNotifications;
import com.github.yannicklamprecht.mc.management.v3_1_0.dto.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ClientNotificationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testNotificationPropertyUnwrapping(boolean positional) throws Exception {
        MinecraftManagementClient client = new MinecraftManagementClient(mapper, URI.create("ws://localhost:25585"), "test");
        MinecraftManagementNotifications notifications = client.notificationsV3_1_0();

        AtomicReference<Player> joinedPlayer = new AtomicReference<>();
        notifications.onPlayersJoined(joinedPlayer::set);

        // Simulate incoming WebSocket text message for player joined
        String incomingJson = "{\"jsonrpc\":\"2.0\",\"method\":\"minecraft:notification/players/joined\",\"params\":{\"player\":{\"id\":\"p-123\",\"name\":\"Notch\"}}}";
        if (positional) {
            incomingJson = """
                    {"jsonrpc":"2.0","method":"minecraft:notification/players/joined",
                     "params":[{"id":"p-123","name":"Notch"}]}
                    """;
        }

        Method processMessageMethod = MinecraftManagementClient.class.getDeclaredMethod("processMessage", String.class);
        processMessageMethod.setAccessible(true);
        processMessageMethod.invoke(client, incomingJson);

        assertNotNull(joinedPlayer.get());
        assertEquals("Notch", joinedPlayer.get().name());
        assertEquals("p-123", joinedPlayer.get().id());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "null"})
    void testNoParamNotification(String params) throws Exception {
        MinecraftManagementClient client = new MinecraftManagementClient(mapper, URI.create("ws://localhost:25585"), "test");
        MinecraftManagementNotifications notifications = client.notificationsV3_1_0();

        AtomicBoolean started = new AtomicBoolean(false);
        notifications.onServerStarted(() -> started.set(true));

        String incomingJson = "{\"jsonrpc\":\"2.0\",\"method\":\"minecraft:notification/server/started\",\"params\":" + params + "}";

        Method processMessageMethod = MinecraftManagementClient.class.getDeclaredMethod("processMessage", String.class);
        processMessageMethod.setAccessible(true);
        processMessageMethod.invoke(client, incomingJson);

        assertTrue(started.get());
    }

    @Test
    void testPositionalScalarNotification() throws Exception {
        var client = new MinecraftManagementClient(mapper, URI.create("ws://localhost:25585"), "test");
        AtomicReference<String> removedIp = new AtomicReference<>();
        client.notificationsV3_1_0().onIpBansRemoved(removedIp::set);
        Method processMessage = MinecraftManagementClient.class.getDeclaredMethod("processMessage", String.class);
        processMessage.setAccessible(true);
        processMessage.invoke(client, """
                {"jsonrpc":"2.0","method":"minecraft:notification/ip_bans/removed","params":["127.0.0.1"]}
                """);
        assertEquals("127.0.0.1", removedIp.get());
    }
}
