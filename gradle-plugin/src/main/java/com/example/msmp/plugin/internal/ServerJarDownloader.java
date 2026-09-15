package com.example.msmp.plugin.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

public final class ServerJarDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerJarDownloader.class);
    private final HttpClient httpClient;
    private final Path cacheRoot;

    public ServerJarDownloader(Path cacheRoot) {
        this.cacheRoot = cacheRoot;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Path downloadServerJar(String versionId, String downloadUrl, String expectedSha1) throws IOException, InterruptedException {
        Path versionDir = cacheRoot.resolve(versionId);
        Files.createDirectories(versionDir);
        Path jarFile = versionDir.resolve("server.jar");

        if (Files.exists(jarFile)) {
            if (expectedSha1 == null || expectedSha1.isBlank() || verifySha1(jarFile, expectedSha1)) {
                LOGGER.info("Using cached server JAR for Minecraft {}", versionId);
                return jarFile;
            }
            LOGGER.info("Cached server JAR for Minecraft {} failed checksum. Re-downloading...", versionId);
            Files.deleteIfExists(jarFile);
        }

        LOGGER.info("Downloading Minecraft {} server JAR from {}...", versionId, downloadUrl);
        Path tempFile = versionDir.resolve("server.jar.tmp");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download server JAR for " + versionId + ": HTTP " + response.statusCode());
        }

        try (InputStream in = response.body()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        if (expectedSha1 != null && !expectedSha1.isBlank() && !verifySha1(tempFile, expectedSha1)) {
            Files.deleteIfExists(tempFile);
            throw new IOException("Downloaded server JAR for " + versionId + " does not match expected SHA-1: " + expectedSha1);
        }

        Files.move(tempFile, jarFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        LOGGER.info("Successfully downloaded Minecraft {} server JAR ({} bytes)", versionId, Files.size(jarFile));
        return jarFile;
    }

    private boolean verifySha1(Path file, String expectedSha1) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            String actualSha1 = HexFormat.of().formatHex(digest.digest());
            return actualSha1.equalsIgnoreCase(expectedSha1);
        } catch (NoSuchAlgorithmException | IOException e) {
            LOGGER.warn("Failed to compute SHA-1 for {}", file, e);
            return false;
        }
    }
}
