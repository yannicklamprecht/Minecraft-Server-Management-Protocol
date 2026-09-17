package com.github.yannicklamprecht.mc.management.plugin;

import com.github.yannicklamprecht.mc.management.plugin.internal.GitIgnoreHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GitIgnoreHelperTest {

    @Test
    void testEnsureIgnoredCreatesFileIfMissing(@TempDir Path tempDir) throws IOException {
        Path targetDir = tempDir.resolve("protocol-schemas");
        Files.createDirectories(targetDir);

        boolean added = GitIgnoreHelper.ensureIgnored(tempDir, targetDir);
        assertTrue(added);

        Path gitIgnore = tempDir.resolve(".gitignore");
        assertTrue(Files.exists(gitIgnore));
        String content = Files.readString(gitIgnore);
        assertTrue(content.contains("protocol-schemas/"));
    }

    @Test
    void testEnsureIgnoredDoesNotDuplicate(@TempDir Path tempDir) throws IOException {
        Path targetDir = tempDir.resolve("protocol-schemas");
        Files.createDirectories(targetDir);

        Path gitIgnore = tempDir.resolve(".gitignore");
        Files.writeString(gitIgnore, "build/\nprotocol-schemas/\n");

        boolean added = GitIgnoreHelper.ensureIgnored(tempDir, targetDir);
        assertFalse(added);

        String content = Files.readString(gitIgnore);
        assertEquals("build/\nprotocol-schemas/\n", content);
    }

    @Test
    void testEnsureIgnoredAppendsCorrectly(@TempDir Path tempDir) throws IOException {
        Path targetDir = tempDir.resolve("custom-schemas");
        Files.createDirectories(targetDir);

        Path gitIgnore = tempDir.resolve(".gitignore");
        Files.writeString(gitIgnore, "build/\n.gradle/");

        boolean added = GitIgnoreHelper.ensureIgnored(tempDir, targetDir);
        assertTrue(added);

        String content = Files.readString(gitIgnore);
        assertTrue(content.contains("custom-schemas/"));
        assertTrue(content.startsWith("build/\n.gradle/\ncustom-schemas/"));
    }

    @Test
    void testEnsureIgnoredSubmodulePath(@TempDir Path tempDir) throws IOException {
        Path submoduleDir = tempDir.resolve("client");
        Path targetDir = submoduleDir.resolve("protocol-schemas");
        Files.createDirectories(targetDir);

        Path gitIgnore = tempDir.resolve(".gitignore");
        Files.writeString(gitIgnore, "build/\n");

        boolean added = GitIgnoreHelper.ensureIgnored(submoduleDir, targetDir);
        assertTrue(added);

        String content = Files.readString(gitIgnore);
        assertTrue(content.contains("client/protocol-schemas/"));
    }

    @Test
    void testEnsureIgnoredWithRootDirectorySearch(@TempDir Path tempDir) throws IOException {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);

        Path subDir = tempDir.resolve("submodule").resolve("schemas");
        Files.createDirectories(subDir);

        boolean added = GitIgnoreHelper.ensureIgnored(null, subDir);
        assertTrue(added);

        Path gitIgnore = tempDir.resolve(".gitignore");
        assertTrue(Files.exists(gitIgnore));
        String content = Files.readString(gitIgnore);
        assertTrue(content.contains("submodule/schemas/"));
    }

    @Test
    void testEnsureIgnoredBuildDirectoryNotAdded(@TempDir Path tempDir) throws IOException {
        Path targetDir = tempDir.resolve("build").resolve("protocol-schemas");
        Files.createDirectories(targetDir);

        Path gitIgnore = tempDir.resolve(".gitignore");
        Files.writeString(gitIgnore, "build/\n");

        boolean added = GitIgnoreHelper.ensureIgnored(tempDir, targetDir);
        assertFalse(added);
    }
}
