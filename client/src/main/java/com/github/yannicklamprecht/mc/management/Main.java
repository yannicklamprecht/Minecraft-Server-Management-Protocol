package com.github.yannicklamprecht.mc.management;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public final class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        new Main();
    }


    public Main() throws InterruptedException {

        String url = System.getenv().getOrDefault("MINECRAFT_MANAGEMENT_URL", "ws://localhost:25585");
        String secret = System.getenv("MINECRAFT_MANAGEMENT_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("MINECRAFT_MANAGEMENT_SECRET is required");
        }

        try (var client = new MinecraftManagementClient(new ObjectMapper(), URI.create(url), secret)) {
            client.connect().join();
            LOGGER.info("Connected to Minecraft Server Management Protocol (MSMP).");

            // Version-agnostic abstraction session (works across 1.0.0, 2.0.0, 3.0.0, 3.1.0)
            MinecraftManagementSession session = client.session();

            // Register version-agnostic notification listener
            session.onPlayerJoined(player ->
                    LOGGER.info("Player joined: {} ({})", player.name(), player.id()));
            session.onServerStatus(status ->
                    LOGGER.info("Server status: started={}, players={}", status.started(), status.players().size()));

            // Call version-agnostic RPC methods
            session.getStatus().thenAccept(state -> {
                LOGGER.info("Server version: {}", state.version() != null ? state.version().name() : "unknown");
                for (var player : state.players()) {
                    LOGGER.info("Online player: {}", player.name());
                }
            }).join();

            session.onPlayerLeft(player ->
                    LOGGER.info("Player left: {} ({})", player.name(), player.id()));

            session.onServerStatus(status ->
                    LOGGER.info("Server status: started={}, players={}", status.started(), status.players().size()));

            session.onServerStopping(() ->
                    LOGGER.info("Server stopping"));
            session.onServerSaved(() ->
                    LOGGER.info("Server saved"));

            session.onServerSaving(() ->
                    LOGGER.info("Server saving"));

            session.onServerActivity(() ->
                    LOGGER.info("Server activity: {}", "Something happened"));

            session.onAllowlistAdded(player ->
                    LOGGER.info("Allowlist added: {} ({})", player.name(), player.id()));

            session.onGameRuleUpdated(gamerule ->
                    LOGGER.info("Gamerule updated: {} = {}", gamerule.key(), gamerule.value()));


            Thread.currentThread().join();
        }
    }

}
