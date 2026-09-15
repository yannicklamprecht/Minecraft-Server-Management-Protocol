package com.example.msmp;

import com.example.msmp.generated.v3_1_0.MinecraftManagementApi;
import com.example.msmp.generated.v3_1_0.MinecraftManagementNotifications;
import com.example.msmp.generated.v3_1_0.dto.Player;
import com.example.msmp.transport.MinecraftManagementClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public final class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String url = System.getenv().getOrDefault("MINECRAFT_MANAGEMENT_URL", "ws://localhost:25585");
        String secret = System.getenv("MINECRAFT_MANAGEMENT_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("MINECRAFT_MANAGEMENT_SECRET is required");
        }

        try (var client = new MinecraftManagementClient(new ObjectMapper(), URI.create(url), secret)) {
            client.connect().join();
            LOGGER.info("Connected to Minecraft Server Management Protocol (MSMP).");

            // Direct version facade access
            MinecraftManagementApi api = client.v3_1_0();
            MinecraftManagementNotifications notifications = client.notificationsV3_1_0();

            // Register notification listener
            notifications.onPlayersJoined(player ->
                    LOGGER.info("Player joined: {} ({})", player.name(), player.id()));
            notifications.onServerStatus(status ->
                    LOGGER.info("Server status: started={}, players={}", status.started(), status.players().size()));

            // Call typed RPC methods
            api.serverStatus().thenAccept(state -> {
                LOGGER.info("Server version: {}", state.version() != null ? state.version().name() : "unknown");
                for (Player player : state.players()) {
                    LOGGER.info("Online player: {}", player.name());
                }
            }).join();
        }
    }
}
