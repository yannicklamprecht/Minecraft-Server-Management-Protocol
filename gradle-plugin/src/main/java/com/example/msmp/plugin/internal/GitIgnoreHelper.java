package com.example.msmp.plugin.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class GitIgnoreHelper {
    private GitIgnoreHelper() {}

    /**
     * Finds the git repository root or project root directory by walking up from the start directory.
     */
    public static Path findGitOrProjectRootDir(Path startDir, Path fallbackRootDir) {
        Path current = startDir != null ? startDir.toAbsolutePath().normalize() : null;
        while (current != null) {
            if (Files.exists(current.resolve(".git")) || Files.exists(current.resolve(".gitignore"))) {
                return current;
            }
            current = current.getParent();
        }
        return fallbackRootDir != null ? fallbackRootDir.toAbsolutePath().normalize() : (startDir != null ? startDir.toAbsolutePath().normalize() : null);
    }

    /**
     * Ensures that the target directory is listed in the root .gitignore file.
     *
     * @param rootDir   the root directory of the project / repository (or fallback)
     * @param targetDir the directory to ignore
     * @return true if an entry was added to .gitignore, false if already present or failed
     */
    public static boolean ensureIgnored(Path rootDir, Path targetDir) {
        if (targetDir == null) {
            return false;
        }

        Path normalizedTarget = targetDir.toAbsolutePath().normalize();
        Path searchBase = rootDir != null ? rootDir.toAbsolutePath().normalize() : normalizedTarget;
        Path resolvedRoot = findGitOrProjectRootDir(searchBase, rootDir != null ? rootDir : searchBase);

        if (resolvedRoot == null) {
            return false;
        }

        Path gitIgnore = resolvedRoot.resolve(".gitignore");
        Path normalizedRoot = resolvedRoot.toAbsolutePath().normalize();

        String relativePath;
        if (normalizedTarget.startsWith(normalizedRoot)) {
            relativePath = normalizedRoot.relativize(normalizedTarget).toString().replace('\\', '/');
        } else {
            // Target is outside project root (e.g. ~/.gradle/caches/...) -> no .gitignore entry needed
            return false;
        }

        if (relativePath.isBlank()) {
            return false;
        }

        if (!relativePath.endsWith("/")) {
            relativePath = relativePath + "/";
        }

        if (relativePath.startsWith("build/") || relativePath.contains("/build/") || relativePath.startsWith(".gradle/")) {
            return false;
        }

        try {
            if (!Files.exists(gitIgnore)) {
                Files.writeString(gitIgnore, relativePath + System.lineSeparator(), StandardOpenOption.CREATE);
                return true;
            }

            List<String> lines = Files.readAllLines(gitIgnore);
            String trimmedPattern = relativePath.endsWith("/") ? relativePath.substring(0, relativePath.length() - 1) : relativePath;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.equals(relativePath)
                        || trimmed.equals("/" + relativePath)
                        || trimmed.equals(trimmedPattern)
                        || trimmed.equals("/" + trimmedPattern)) {
                    return false;
                }
                if (trimmed.endsWith("/")) {
                    String prefix = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
                    if (relativePath.startsWith(prefix) || relativePath.contains("/" + prefix)) {
                        return false;
                    }
                }
            }

            String content = Files.readString(gitIgnore);
            StringBuilder sb = new StringBuilder();
            if (!content.isEmpty() && !content.endsWith("\n") && !content.endsWith("\r")) {
                sb.append(System.lineSeparator());
            }
            sb.append(relativePath).append(System.lineSeparator());
            Files.writeString(gitIgnore, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
