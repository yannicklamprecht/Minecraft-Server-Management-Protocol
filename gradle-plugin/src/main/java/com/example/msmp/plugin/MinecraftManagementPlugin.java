package com.example.msmp.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MinecraftManagementPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "minecraftManagement";
    public static final String TASK_NAME = "extractMinecraftManagementSchemas";

    @Override
    public void apply(Project project) {
        MinecraftManagementExtension extension = project.getExtensions().create(
                EXTENSION_NAME,
                MinecraftManagementExtension.class
        );

        extension.getOutputDir().convention(project.getLayout().getProjectDirectory().dir("protocol-schemas"));
        extension.getCacheDir().convention(project.getLayout().getBuildDirectory().dir("minecraft-servers"));

        project.getTasks().register(TASK_NAME, ExtractMinecraftManagementSchemasTask.class, task -> {
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

        // Register alias task for convenience
        project.getTasks().register("extractProtocolSchemas", task -> {
            task.setGroup("minecraft management");
            task.setDescription("Alias for " + TASK_NAME);
            task.dependsOn(TASK_NAME);
        });
    }
}
