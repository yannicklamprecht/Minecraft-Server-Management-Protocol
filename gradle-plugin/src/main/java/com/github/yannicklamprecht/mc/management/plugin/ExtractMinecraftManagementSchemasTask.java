package com.github.yannicklamprecht.mc.management.plugin;

import com.github.yannicklamprecht.mc.management.plugin.generator.OpenRpcCodeGenerator;
import com.github.yannicklamprecht.mc.management.plugin.internal.JavaRuntimeLocator;
import com.github.yannicklamprecht.mc.management.plugin.internal.MojangManifestService;
import com.github.yannicklamprecht.mc.management.plugin.internal.ServerDataGenerator;
import com.github.yannicklamprecht.mc.management.plugin.internal.ServerJarDownloader;
import tools.jackson.databind.ObjectMapper;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CacheableTask
public abstract class ExtractMinecraftManagementSchemasTask extends DefaultTask {

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getManifestUrl();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<Boolean> getOnlyReleases();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract ListProperty<String> getVersions();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getMinMinecraftVersion();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getJavaExecutable();

    @Internal
    public abstract DirectoryProperty getCacheDir();

    @Internal
    public abstract DirectoryProperty getProjectRootDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void extract() throws Exception {
        String manifestUrl = getManifestUrl().getOrElse(MinecraftManagementExtension.DEFAULT_MANIFEST_URL);
        boolean onlyReleases = getOnlyReleases().getOrElse(true);
        List<String> configuredVersions = getVersions().getOrElse(List.of());
        String configuredJava = getJavaExecutable().getOrNull();
        File cacheDirFile = getCacheDir().get().getAsFile();
        File outputDirFile = getOutputDir().get().getAsFile();
        String minVersion = getMinMinecraftVersion().getOrNull();

        if (!outputDirFile.exists()) {
            if (outputDirFile.mkdirs()) {
                getLogger().lifecycle("Created output directory: {}", outputDirFile.getAbsolutePath());
            }
        }

        // Ensure schema generation directory is in .gitignore
        File rootDirFile = getProjectRootDir().isPresent()
                ? getProjectRootDir().get().getAsFile()
                : outputDirFile.getParentFile();
        if (rootDirFile != null && com.github.yannicklamprecht.mc.management.plugin.internal.GitIgnoreHelper.ensureIgnored(rootDirFile.toPath(), outputDirFile.toPath())) {
            getLogger().lifecycle("Added schema output directory to .gitignore");
        }

        getLogger().lifecycle("Fetching Minecraft versions manifest from {}...", manifestUrl);
        MojangManifestService manifestService = new MojangManifestService(manifestUrl);
        List<MojangManifestService.VersionEntry> allVersions = manifestService.fetchVersions(onlyReleases, configuredVersions);

        List<MojangManifestService.VersionEntry> versions = new ArrayList<>();
        for (MojangManifestService.VersionEntry v : allVersions) {
            versions.add(v);
            if (configuredVersions.isEmpty() && minVersion != null && minVersion.equalsIgnoreCase(v.id())) {
                break;
            }
        }

        getLogger().lifecycle("Found {} matching Minecraft version candidate(s) to check for management protocol schemas.", versions.size());

        ServerJarDownloader downloader = new ServerJarDownloader(cacheDirFile.toPath());
        Path workRootDir = cacheDirFile.toPath().resolve("work");

        Map<String, List<String>> protocolToMinecraftVersions = new LinkedHashMap<>();
        int processedCount = 0;
        int schemasFoundCount = 0;

        for (MojangManifestService.VersionEntry versionEntry : versions) {
            String vid = versionEntry.id();
            processedCount++;

            getLogger().lifecycle("[{}/{}] Checking Minecraft {} ({})", processedCount, versions.size(), vid, versionEntry.type());

            Path versionCacheDir = cacheDirFile.toPath().resolve(vid);
            Path cachedSchemaFile = versionCacheDir.resolve("openrpc.json");
            Path noSchemaMarkerFile = versionCacheDir.resolve(".no-schema");

            // Fast-path: Check if we have cached schema or known no-schema result for this Minecraft version
            if (Files.exists(cachedSchemaFile)) {
                try {
                    var extracted = ServerDataGenerator.loadSchemaFromFile(cachedSchemaFile, vid);
                    String protocolVer = extracted.protocolVersion();
                    Path targetSchemaFile = outputDirFile.toPath().resolve(protocolVer).resolve("openrpc.json");

                    if (!Files.exists(targetSchemaFile)) {
                        ServerDataGenerator.writeFormattedSchema(extracted.schemaContent(), targetSchemaFile);
                    }
                    protocolToMinecraftVersions.computeIfAbsent(protocolVer, k -> new ArrayList<>()).add(vid);
                    schemasFoundCount++;

                    getLogger().lifecycle("--> Reusing cached OpenRPC v{} schema for Minecraft {} -> {}",
                            protocolVer, vid, rootDirFile.toPath().relativize(targetSchemaFile));
                    continue;
                } catch (Exception e) {
                    getLogger().warn("Failed to read cached schema for Minecraft {}: {}. Re-extracting...", vid, e.getMessage());
                }
            } else if (Files.exists(noSchemaMarkerFile)) {
                getLogger().info("Using cached result for Minecraft {}: no management protocol schema", vid);
                if (configuredVersions.isEmpty() && minVersion != null && minVersion.equalsIgnoreCase(vid)) {
                    getLogger().lifecycle("Reached minMinecraftVersion '{}'. Stopping search for older releases.", minVersion);
                    break;
                }
                continue;
            }

            var serverDownloadOpt = manifestService.fetchServerDownload(versionEntry.url());
            if (serverDownloadOpt.isEmpty()) {
                getLogger().info("No server download available for Minecraft {}", vid);
                continue;
            }

            var serverDownload = serverDownloadOpt.get();
            Path serverJar;
            try {
                serverJar = downloader.downloadServerJar(vid, serverDownload.url(), serverDownload.sha1());
            } catch (Exception e) {
                getLogger().warn("Failed to download server JAR for Minecraft {}: {}", vid, e.getMessage());
                continue;
            }

            int requiredJava = serverDownload.javaMajorVersion();
            String javaExec = JavaRuntimeLocator.locateJava(requiredJava, configuredJava);

            var extractedSchemaOpt = ServerDataGenerator.extractSchema(
                    vid,
                    serverJar,
                    javaExec,
                    workRootDir
            );

            if (extractedSchemaOpt.isPresent()) {
                var extracted = extractedSchemaOpt.get();
                String protocolVer = extracted.protocolVersion();

                // Cache the extracted schema for this Minecraft version
                Files.createDirectories(versionCacheDir);
                ServerDataGenerator.writeFormattedSchema(extracted.schemaContent(), cachedSchemaFile);

                // Write to outputDir under protocol version
                Path targetSchemaFile = outputDirFile.toPath().resolve(protocolVer).resolve("openrpc.json");
                ServerDataGenerator.writeFormattedSchema(extracted.schemaContent(), targetSchemaFile);

                protocolToMinecraftVersions.computeIfAbsent(protocolVer, k -> new ArrayList<>()).add(vid);
                schemasFoundCount++;

                getLogger().lifecycle("--> Extracted OpenRPC v{} schema from Minecraft {} -> {}",
                        protocolVer, vid, rootDirFile.toPath().relativize(targetSchemaFile));
            } else {
                // Cache .no-schema marker to prevent re-running data generator on future executions
                Files.createDirectories(versionCacheDir);
                Files.writeString(noSchemaMarkerFile, "");

                getLogger().info("No management protocol schema generated for Minecraft {}", vid);
                if (configuredVersions.isEmpty() && minVersion != null && minVersion.equalsIgnoreCase(vid)) {
                    getLogger().lifecycle("Reached minMinecraftVersion '{}'. Stopping search for older releases.", minVersion);
                    break;
                }
            }
        }

        getLogger().lifecycle("\n=======================================================");
        getLogger().lifecycle("Minecraft Management Protocol Schemas Extraction Summary");
        getLogger().lifecycle("=======================================================");
        if (protocolToMinecraftVersions.isEmpty()) {
            getLogger().warn("No management protocol schemas were extracted.");
        } else {
            for (var entry : protocolToMinecraftVersions.entrySet()) {
                getLogger().lifecycle("OpenRPC Version {}: derived from Minecraft {}",
                        entry.getKey(), String.join(", ", entry.getValue()));
            }
            getLogger().lifecycle("Total protocol schemas written to: {}", outputDirFile.getAbsolutePath());
        }
        getLogger().lifecycle("=======================================================\n");

        writeMinecraftVersionMapping(outputDirFile, protocolToMinecraftVersions);
    }

    /**
     * Persists the Minecraft-version-to-protocol-version mapping discovered above as JSON, so
     * {@link com.github.yannicklamprecht.mc.management.plugin.generator.OpenRpcCodeGenerator} can turn it into a generated
     * {@code ProtocolVersions} class without needing to re-derive it from server jars itself.
     */
    private void writeMinecraftVersionMapping(File outputDirFile, Map<String, List<String>> protocolToMinecraftVersions) throws IOException {
        Map<String, String> minecraftVersionToProtocol = new LinkedHashMap<>();
        for (var entry : protocolToMinecraftVersions.entrySet()) {
            for (String minecraftVersion : entry.getValue()) {
                minecraftVersionToProtocol.put(minecraftVersion, entry.getKey());
            }
        }
        File mappingFile = new File(outputDirFile, OpenRpcCodeGenerator.MINECRAFT_VERSION_MAPPING_FILE_NAME);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(mappingFile, minecraftVersionToProtocol);
        getLogger().lifecycle("Wrote Minecraft version -> protocol version mapping to {}", mappingFile.getAbsolutePath());
    }
}
