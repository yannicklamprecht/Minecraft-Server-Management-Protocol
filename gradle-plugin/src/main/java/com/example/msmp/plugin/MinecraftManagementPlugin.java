package com.example.msmp.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MinecraftManagementPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "minecraftManagement";
    public static final String EXTRACT_TASK_NAME = "extractMinecraftManagementSchemas";
    public static final String GENERATE_TASK_NAME = "generateMinecraftManagementSources";

    @Override
    public void apply(Project project) {
        MinecraftManagementExtension extension = project.getExtensions().create(
                EXTENSION_NAME,
                MinecraftManagementExtension.class
        );

        extension.getOutputDir().convention(project.getLayout().getProjectDirectory().dir("protocol-schemas"));
        extension.getSchemasDir().convention(extension.getOutputDir());
        extension.getCacheDir().convention(project.getLayout().getBuildDirectory().dir("minecraft-servers"));

        if (project.file("client").isDirectory()) {
            extension.getGeneratedSourcesDir().convention(project.getLayout().getProjectDirectory().dir("client/src/generated/java"));
        } else {
            extension.getGeneratedSourcesDir().convention(project.getLayout().getProjectDirectory().dir("src/generated/java"));
        }

        project.getTasks().register(EXTRACT_TASK_NAME, ExtractMinecraftManagementSchemasTask.class, task -> {
            task.setGroup("minecraft management");
            task.setDescription("Downloads Minecraft server JARs, executes data generation, and extracts OpenRPC schemas for the Management Protocol.");

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

        project.getTasks().register(GENERATE_TASK_NAME, GenerateMinecraftManagementSourcesTask.class, task -> {
            task.setGroup("minecraft management");
            task.setDescription("Generates typed Java DTOs, records, enums, and API facades from OpenRPC schemas.");

            task.getSchemasDir().convention(extension.getSchemasDir());
            task.getOutputDir().convention(extension.getGeneratedSourcesDir());
            task.getPackageName().convention(extension.getPackageName());
            task.getClientClassName().convention(extension.getClientClassName());
        });

        // Register alias tasks for generation
        project.getTasks().register("generateMsmpSources", task -> {
            task.setGroup("minecraft management");
            task.setDescription("Alias for " + GENERATE_TASK_NAME);
            task.dependsOn(GENERATE_TASK_NAME);
        });

        project.getTasks().register("generateProtocolSources", task -> {
            task.setGroup("minecraft management");
            task.setDescription("Alias for " + GENERATE_TASK_NAME);
            task.dependsOn(GENERATE_TASK_NAME);
        });
    }
}
