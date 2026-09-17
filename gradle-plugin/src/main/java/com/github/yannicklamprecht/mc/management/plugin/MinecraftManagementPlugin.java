package com.github.yannicklamprecht.mc.management.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class MinecraftManagementPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "minecraftManagement";
    public static final String EXTRACT_TASK_NAME = "extractMinecraftManagementSchemas";
    public static final String GENERATE_TASK_NAME = "generateMinecraftManagementSources";
    public static final String CLEAN_CACHE_TASK_NAME = "cleanMinecraftManagementCache";
    public static final String CLEAN_CACHE_ALIAS_TASK_NAME = "cleanCache";
    public static final String CLEAN_SOURCES_TASK_NAME = "cleanMinecraftManagementSources";
    public static final String CLEAN_SOURCES_ALIAS_TASK_NAME = "cleanGeneratedSources";

    /**
     * Project property that forces (when {@code true}) or suppresses (when {@code false}) wiring
     * {@link #GENERATE_TASK_NAME} into {@code compileJava}, overriding the default of only doing
     * so when {@code generatedSourcesDir} has no generated {@code .java} files yet. Pass with
     * {@code -PminecraftManagement.autoGenerate=true} (or {@code false}).
     */
    public static final String AUTO_GENERATE_PROPERTY = "minecraftManagement.autoGenerate";

    @Override
    public void apply(Project project) {
        MinecraftManagementExtension extension = project.getExtensions().create(
                EXTENSION_NAME,
                MinecraftManagementExtension.class
        );

        // Configure project-specific cache under <rootDir>/.gradle/caches/minecraft-management-gradle-plugin
        Directory projectGradleCacheDir = project.getRootProject().getLayout().getProjectDirectory().dir(".gradle/caches/minecraft-management-gradle-plugin");

        extension.getOutputDir().convention(projectGradleCacheDir.dir("protocol-schemas"));
        extension.getSchemasDir().convention(extension.getOutputDir());
        extension.getCacheDir().convention(projectGradleCacheDir.dir("minecraft-servers"));
        extension.getGeneratedSourcesDir().convention(project.getLayout().getProjectDirectory().dir("src/generated/java"));

        TaskProvider<ExtractMinecraftManagementSchemasTask> extractTask = project.getTasks().register(EXTRACT_TASK_NAME, ExtractMinecraftManagementSchemasTask.class, task -> {
            task.setGroup("minecraft management");
            task.setDescription("Downloads Minecraft server JARs, executes data generation, and extracts OpenRPC schemas for the Management Protocol.");

            task.getProjectRootDir().convention(project.getRootProject().getLayout().getProjectDirectory());
            task.getManifestUrl().convention(extension.getManifestUrl());
            task.getOnlyReleases().convention(extension.getOnlyReleases());
            task.getVersions().convention(extension.getVersions());
            task.getMinMinecraftVersion().convention(extension.getMinMinecraftVersion());
            task.getJavaExecutable().convention(extension.getJavaExecutable());
            task.getOutputDir().convention(extension.getOutputDir());
            task.getCacheDir().convention(extension.getCacheDir());
        });

        // Register alias tasks for extraction
        project.getTasks().register("extractProtocolSchemas", task -> {
            task.setGroup("minecraft management");
            task.setDescription("Alias for " + EXTRACT_TASK_NAME);
            task.dependsOn(EXTRACT_TASK_NAME);
        });

        TaskProvider<GenerateMinecraftManagementSourcesTask> generateTask = project.getTasks().register(
                GENERATE_TASK_NAME,
                GenerateMinecraftManagementSourcesTask.class,
                task -> {
                    task.setGroup("minecraft management");
                    task.setDescription("Generates typed Java DTOs, records, enums, and API facades from OpenRPC schemas.");

                    task.getProjectRootDir().convention(project.getRootProject().getLayout().getProjectDirectory());
                    task.getSchemasDir().convention(extension.getSchemasDir());
                    task.getOutputDir().convention(extension.getGeneratedSourcesDir());
                    task.getPackageName().convention(extension.getPackageName());
                    task.getClientClassName().convention(extension.getClientClassName());
                    task.dependsOn(extractTask);
                }
        );

        // Register alias tasks for generation
        project.getTasks().register("generateMsmpSources", task -> {
            task.setGroup("minecraft management");
            task.setDescription("Alias for " + GENERATE_TASK_NAME);
            task.dependsOn(generateTask);
        });

        project.getTasks().register("generateProtocolSources", task -> {
            task.setGroup("minecraft management");
            task.setDescription("Alias for " + GENERATE_TASK_NAME);
            task.dependsOn(generateTask);
        });

        // Register clean cache tasks
        TaskProvider<Delete> cleanCacheTask = project.getTasks().register(CLEAN_CACHE_TASK_NAME, Delete.class, task -> {
            task.setGroup("minecraft management");
            task.setDescription("Cleans the Minecraft Management plugin cache (downloaded server JARs and extracted schemas).");
            task.delete(extension.getCacheDir(), extension.getOutputDir());
        });

        if (!project.getTasks().getNames().contains(CLEAN_CACHE_ALIAS_TASK_NAME)) {
            project.getTasks().register(CLEAN_CACHE_ALIAS_TASK_NAME, task -> {
                task.setGroup("minecraft management");
                task.setDescription("Alias for " + CLEAN_CACHE_TASK_NAME);
                task.dependsOn(cleanCacheTask);
            });
        }

        // Register clean generated sources tasks
        TaskProvider<Delete> cleanSourcesTask = project.getTasks().register(CLEAN_SOURCES_TASK_NAME, Delete.class, task -> {
            task.setGroup("minecraft management");
            task.setDescription("Cleans the generated Minecraft Management Java sources.");
            task.delete(extension.getGeneratedSourcesDir());
        });

        if (!project.getTasks().getNames().contains(CLEAN_SOURCES_ALIAS_TASK_NAME)) {
            project.getTasks().register(CLEAN_SOURCES_ALIAS_TASK_NAME, task -> {
                task.setGroup("minecraft management");
                task.setDescription("Alias for " + CLEAN_SOURCES_TASK_NAME);
                task.dependsOn(cleanSourcesTask);
            });
        }

        // If Java plugin is applied (e.g. in a submodule or client project), wire sourceSets and compilation
        project.getPlugins().withId("java", plugin -> {
            JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet mainSourceSet = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            mainSourceSet.getJava().srcDir(extension.getGeneratedSourcesDir());

            project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME).configure(task ->
                    task.dependsOn(project.provider(() ->
                            shouldAutoGenerate(project, extension) ? List.of(generateTask) : Collections.emptyList()
                    ))
            );
        });
    }

    /**
     * Decides whether {@code compileJava} (and therefore {@code build}) should trigger schema
     * extraction/generation - which downloads Minecraft server JARs and runs their data generators -
     * automatically. Controlled by {@link #AUTO_GENERATE_PROPERTY} when set; otherwise defaults to
     * only bootstrapping when {@code generatedSourcesDir} has no generated {@code .java} files yet,
     * so a normal build never re-downloads/re-runs generation against sources already on disk
     * (e.g. checked into version control).
     */
    private static boolean shouldAutoGenerate(Project project, MinecraftManagementExtension extension) {
        Object override = project.findProperty(AUTO_GENERATE_PROPERTY);
        if (override != null) {
            return Boolean.parseBoolean(override.toString());
        }
        return !hasGeneratedSources(extension.getGeneratedSourcesDir().get().getAsFile());
    }

    private static boolean hasGeneratedSources(File generatedSourcesDir) {
        if (!generatedSourcesDir.exists()) {
            return false;
        }
        Path root = generatedSourcesDir.toPath();
        try (var stream = Files.walk(root)) {
            return stream.anyMatch(path -> path.toString().endsWith(".java"));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to inspect generated sources directory: " + root, e);
        }
    }
}
