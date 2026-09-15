package com.example.msmp.plugin;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PluginConfigurationTest {

    @Test
    void testPluginAppliesProjectLocalConventions(@TempDir File projectDir) throws IOException {
        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
        project.getPlugins().apply(MinecraftManagementPlugin.class);

        MinecraftManagementExtension ext = project.getExtensions().getByType(MinecraftManagementExtension.class);

        File expectedSharedCache = new File(project.getGradle().getGradleUserHomeDir(), "caches/msmp");
        assertEquals(new File(expectedSharedCache, "protocol-schemas").getCanonicalFile(), ext.getOutputDir().get().getAsFile().getCanonicalFile());
        assertEquals(new File(expectedSharedCache, "protocol-schemas").getCanonicalFile(), ext.getSchemasDir().get().getAsFile().getCanonicalFile());
        assertEquals(new File(expectedSharedCache, "minecraft-servers").getCanonicalFile(), ext.getCacheDir().get().getAsFile().getCanonicalFile());
        assertEquals(new File(projectDir, "src/generated/java").getCanonicalFile(), ext.getGeneratedSourcesDir().get().getAsFile().getCanonicalFile());
        assertFalse(ext.getPackageName().isPresent(), "packageName should not be defaulted so user is forced to configure it");
    }

    @Test
    void testMissingPackageNameThrowsException(@TempDir File projectDir) {
        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
        project.getPlugins().apply(MinecraftManagementPlugin.class);

        GenerateMinecraftManagementSourcesTask task = project.getTasks()
                .named(MinecraftManagementPlugin.GENERATE_TASK_NAME, GenerateMinecraftManagementSourcesTask.class)
                .get();

        assertThrows(InvalidUserDataException.class, task::generate);
    }
}
