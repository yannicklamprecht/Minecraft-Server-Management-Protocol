package com.example.msmp;

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
            LOGGER.info("Connected. Instantiate a generated protocol facade from com.example.msmp.generated.<version>.");
        }
    }
}
