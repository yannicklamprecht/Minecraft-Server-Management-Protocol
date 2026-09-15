package com.example.msmp.plugin;

import com.example.msmp.plugin.generator.OpenRpcCodeGenerator;
import com.example.msmp.plugin.internal.GitIgnoreHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;

public abstract class GenerateMinecraftManagementSourcesTask extends DefaultTask {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSchemasDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getPackageName();

    @Input
    @Optional
    public abstract Property<String> getClientClassName();

    @Internal
    public abstract DirectoryProperty getProjectRootDir();

    @TaskAction
    public void generate() throws IOException {
        if (!getPackageName().isPresent() || getPackageName().get().trim().isEmpty()) {
            throw new InvalidUserDataException(
                    "The 'packageName' property in 'minecraftManagement' extension must be configured (e.g. packageName.set(\"com.example.msmp.generated\"))."
            );
        }

        File schemasDir = getSchemasDir().get().getAsFile();
        if (!schemasDir.exists()) {
            if (schemasDir.mkdirs()) {
                getLogger().lifecycle("Created missing schemas directory: {}", schemasDir.getAbsolutePath());
            }
        }

        // Ensure schema directory is in .gitignore
        File rootDirFile = getProjectRootDir().isPresent()
                ? getProjectRootDir().get().getAsFile()
                : schemasDir.getParentFile();
        if (rootDirFile != null && GitIgnoreHelper.ensureIgnored(rootDirFile.toPath(), schemasDir.toPath())) {
            getLogger().lifecycle("Added schema directory to .gitignore");
        }

        File outputDir = getOutputDir().get().getAsFile();
        String basePackage = getPackageName().get().trim();
        String clientClass = getClientClassName().getOrElse("com.example.msmp.transport.MinecraftManagementClient");

        getLogger().lifecycle("Generating MSMP Java sources from {} into {} (package: {})",
                schemasDir.getAbsolutePath(), outputDir.getAbsolutePath(), basePackage);

        OpenRpcCodeGenerator generator = new OpenRpcCodeGenerator(clientClass);
        generator.generateAll(schemasDir.toPath(), outputDir.toPath(), basePackage);
    }
}
