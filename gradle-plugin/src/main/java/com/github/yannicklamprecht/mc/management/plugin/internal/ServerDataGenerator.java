package com.github.yannicklamprecht.mc.management.plugin.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public final class ServerDataGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerDataGenerator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectWriter PRETTY_WRITER = MAPPER.writerWithDefaultPrettyPrinter();

    private ServerDataGenerator() {}

    public record ExtractedSchema(String protocolVersion, String openRpcTitle, Map<String, Object> schemaContent) {}

    public static Optional<ExtractedSchema> extractSchema(
            String versionId,
            Path serverJar,
            String javaExecutable,
            Path workRootDir
    ) throws IOException, InterruptedException {
        Path workDir = workRootDir.resolve("work-" + versionId);
        deleteRecursively(workDir);
        Files.createDirectories(workDir);

        Path outputDir = workDir.resolve("generated");
        Files.createDirectories(outputDir);

        LOGGER.info("Running data generator for Minecraft {} using Java '{}'...", versionId, javaExecutable);

        ProcessBuilder pb = new ProcessBuilder(
                javaExecutable,
                "-DbundlerMainClass=net.minecraft.data.Main",
                "-jar",
                serverJar.toAbsolutePath().toString(),
                "--reports",
                "--output",
                outputDir.toAbsolutePath().toString()
        );
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder logOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logOutput.append(line).append(System.lineSeparator());
                LOGGER.debug("[MC {} DataGen] {}", versionId, line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            LOGGER.debug("Data generator exited with code {} for Minecraft {}. Output:\n{}", exitCode, versionId, logOutput);
        }

        Path reportsDir = outputDir.resolve("reports");
        Path schemaFile = reportsDir.resolve("json-rpc-api-schema.json");
        if (!Files.exists(schemaFile)) {
            schemaFile = reportsDir.resolve("openrpc.json");
        }

        if (!Files.exists(schemaFile)) {
            LOGGER.debug("No management protocol schema generated for Minecraft {}", versionId);
            deleteRecursively(workDir);
            return Optional.empty();
        }

        LOGGER.info("Found management protocol schema in Minecraft {} reports!", versionId);
        Map<String, Object> schemaMap = MAPPER.readValue(schemaFile.toFile(), new TypeReference<>() {});

        String protocolVersion = extractProtocolVersion(schemaMap, versionId);
        String title = extractTitle(schemaMap);

        deleteRecursively(workDir);

        return Optional.of(new ExtractedSchema(protocolVersion, title, schemaMap));
    }

    public static ExtractedSchema loadSchemaFromFile(Path schemaFile, String fallbackVersion) throws IOException {
        Map<String, Object> schemaMap = MAPPER.readValue(schemaFile.toFile(), new TypeReference<>() {});
        String protocolVersion = extractProtocolVersion(schemaMap, fallbackVersion);
        String title = extractTitle(schemaMap);
        return new ExtractedSchema(protocolVersion, title, schemaMap);
    }

    public static void writeFormattedSchema(Map<String, Object> schemaMap, Path destinationFile) throws IOException {
        Files.createDirectories(destinationFile.getParent());
        PRETTY_WRITER.writeValue(destinationFile.toFile(), schemaMap);
    }

    @SuppressWarnings("unchecked")
    private static String extractProtocolVersion(Map<String, Object> schema, String fallback) {
        Object infoObj = schema.get("info");
        if (infoObj instanceof Map<?, ?> infoMap) {
            Object ver = infoMap.get("version");
            if (ver != null && !String.valueOf(ver).isBlank()) {
                return String.valueOf(ver);
            }
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static String extractTitle(Map<String, Object> schema) {
        Object infoObj = schema.get("info");
        if (infoObj instanceof Map<?, ?> infoMap) {
            Object title = infoMap.get("title");
            if (title != null) return String.valueOf(title);
        }
        return "Minecraft Server JSON-RPC";
    }

    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) return;
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}
    }
}
