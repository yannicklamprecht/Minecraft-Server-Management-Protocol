package com.example.msmp.plugin.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class JavaRuntimeLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaRuntimeLocator.class);

    private JavaRuntimeLocator() {}

    public static String locateJava(int requiredMajorVersion, String configuredJava) {
        if (configuredJava != null && !configuredJava.isBlank()) {
            File configuredFile = new File(configuredJava);
            if (configuredFile.exists()) {
                return configuredFile.getAbsolutePath();
            }
            return configuredJava;
        }

        int currentMajor = Runtime.version().feature();
        if (currentMajor >= requiredMajorVersion) {
            String javaHome = System.getProperty("java.home");
            Path javaBin = Path.of(javaHome, "bin", isWindows() ? "java.exe" : "java");
            if (Files.exists(javaBin)) {
                return javaBin.toAbsolutePath().toString();
            }
        }

        List<Path> candidateRoots = new ArrayList<>();

        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            Path sdkmanJava = Path.of(userHome, ".sdkman", "candidates", "java");
            addChildren(sdkmanJava, candidateRoots);

            Path asdfJava = Path.of(userHome, ".asdf", "installs", "java");
            addChildren(asdfJava, candidateRoots);

            Path jenvJava = Path.of(userHome, ".jenv", "versions");
            addChildren(jenvJava, candidateRoots);
        }

        if (isMac()) {
            Path macJvms = Path.of("/Library/Java/JavaVirtualMachines");
            if (Files.isDirectory(macJvms)) {
                try (var stream = Files.list(macJvms)) {
                    stream.forEach(dir -> {
                        Path home = dir.resolve("Contents/Home");
                        if (Files.isDirectory(home)) candidateRoots.add(home);
                        else if (Files.isDirectory(dir)) candidateRoots.add(dir);
                    });
                } catch (IOException ignored) {}
            }
        } else if (isLinux()) {
            Path linuxJvms = Path.of("/usr/lib/jvm");
            addChildren(linuxJvms, candidateRoots);
        } else if (isWindows()) {
            addChildren(Path.of("C:\\Program Files\\Java"), candidateRoots);
            addChildren(Path.of("C:\\Program Files\\Eclipse Adoptium"), candidateRoots);
        }

        String envJavaHome = System.getenv("JAVA_HOME");
        if (envJavaHome != null && !envJavaHome.isBlank()) {
            candidateRoots.add(Path.of(envJavaHome));
        }

        Path bestCandidate = null;
        int bestVersion = -1;

        for (Path root : candidateRoots) {
            Path javaBin = root.resolve("bin").resolve(isWindows() ? "java.exe" : "java");
            if (!Files.isExecutable(javaBin)) continue;

            int version = detectJavaMajorVersion(root);
            if (version >= requiredMajorVersion) {
                if (bestCandidate == null || (version >= requiredMajorVersion && version < bestVersion)) {
                    bestCandidate = javaBin;
                    bestVersion = version;
                }
            }
        }

        if (bestCandidate != null) {
            LOGGER.debug("Located Java {} for required version {} at {}", bestVersion, requiredMajorVersion, bestCandidate);
            return bestCandidate.toAbsolutePath().toString();
        }

        LOGGER.warn("Could not find installed Java >= {}. Falling back to default 'java'.", requiredMajorVersion);
        return "java";
    }

    private static void addChildren(Path parent, List<Path> list) {
        if (Files.isDirectory(parent)) {
            try (var stream = Files.list(parent)) {
                stream.filter(Files::isDirectory).forEach(list::add);
            } catch (IOException ignored) {}
        }
    }

    private static int detectJavaMajorVersion(Path javaHome) {
        Path releaseFile = javaHome.resolve("release");
        if (Files.isRegularFile(releaseFile)) {
            try {
                for (String line : Files.readAllLines(releaseFile)) {
                    if (line.startsWith("JAVA_VERSION=")) {
                        String verStr = line.substring("JAVA_VERSION=".length()).replace("\"", "").replace("'", "").trim();
                        return parseMajorVersion(verStr);
                    }
                }
            } catch (Exception ignored) {}
        }
        return -1;
    }

    private static int parseMajorVersion(String versionString) {
        if (versionString.startsWith("1.")) {
            versionString = versionString.substring(2);
        }
        int dot = versionString.indexOf('.');
        int dash = versionString.indexOf('-');
        int end = versionString.length();
        if (dot != -1) end = Math.min(end, dot);
        if (dash != -1) end = Math.min(end, dash);
        try {
            return Integer.parseInt(versionString.substring(0, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }
}
