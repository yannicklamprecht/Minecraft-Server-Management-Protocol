package com.example.msmp.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;

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

        // Configure schemas / output dir convention for this project
        extension.getOutputDir().convention(project.getLayout().getProjectDirectory().dir("protocol-schemas"));
        extension.getSchemasDir().convention(extension.getOutputDir());
        extension.getGeneratedSourcesDir().convention(project.getLayout().getProjectDirectory().dir("src/generated/java"));
        extension.getCacheDir().convention(project.getLayout().getBuildDirectory().dir("minecraft-servers"));

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

        // If Java plugin is applied (e.g. in a submodule or client project), wire sourceSets and compilation
        project.getPlugins().withId("java", plugin -> {
            JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet mainSourceSet = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            mainSourceSet.getJava().srcDir(extension.getGeneratedSourcesDir());

            project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME).configure(task -> {
                task.dependsOn(generateTask);
            });
        });

        // Wire clean task if base plugin is present
        project.getPlugins().withId("base", plugin -> {
            project.getTasks().named(BasePlugin.CLEAN_TASK_NAME, Delete.class).configure(task -> {
                task.delete(extension.getGeneratedSourcesDir());
            });
        });
    }
}
