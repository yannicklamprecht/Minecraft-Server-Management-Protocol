package com.example.msmp.plugin;

import com.example.msmp.plugin.generator.OpenRpcCodeGenerator;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;

public abstract class GenerateMinecraftManagementSourcesTask extends DefaultTask {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSchemasDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getPackageName();

    @Input
    @Optional
    public abstract Property<String> getClientClassName();

    @TaskAction
    public void generate() throws IOException {
        File schemasDir = getSchemasDir().get().getAsFile();
        File outputDir = getOutputDir().get().getAsFile();
        String basePackage = getPackageName().get();
        String clientClass = getClientClassName().getOrElse("com.example.msmp.transport.MinecraftManagementClient");

        getLogger().lifecycle("Generating MSMP Java sources from {} into {} (package: {})",
                schemasDir.getAbsolutePath(), outputDir.getAbsolutePath(), basePackage);

        OpenRpcCodeGenerator generator = new OpenRpcCodeGenerator(clientClass);
        generator.generateAll(schemasDir.toPath(), outputDir.toPath(), basePackage);
    }
}
