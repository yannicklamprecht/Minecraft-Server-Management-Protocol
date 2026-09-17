package com.github.yannicklamprecht.mc.management.plugin.internal;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public final class MojangManifestService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient httpClient;
    private final String manifestUrl;

    public MojangManifestService(String manifestUrl) {
        this.manifestUrl = manifestUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public record VersionEntry(String id, String type, String url, String time, String releaseTime) {}
    public record ServerDownload(String url, String sha1, long size, int javaMajorVersion) {}

    public List<VersionEntry> fetchVersions(boolean onlyReleases, List<String> explicitVersions) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(manifestUrl))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch version manifest: HTTP " + response.statusCode());
        }

        Map<String, Object> manifest = MAPPER.readValue(response.body(), new TypeReference<>() {});
        List<?> rawVersions = (List<?>) manifest.getOrDefault("versions", List.of());

        List<VersionEntry> result = new ArrayList<>();
        Set<String> explicitSet = (explicitVersions == null || explicitVersions.isEmpty())
                ? Set.of()
                : new HashSet<>(explicitVersions);

        for (Object item : rawVersions) {
            if (!(item instanceof Map<?, ?> map)) continue;
            String id = String.valueOf(map.get("id"));
            String type = String.valueOf(map.get("type"));
            String url = String.valueOf(map.get("url"));
            String time = String.valueOf(map.get("time"));
            String releaseTime = String.valueOf(map.get("releaseTime"));

            if (!explicitSet.isEmpty()) {
                if (explicitSet.contains(id)) {
                    result.add(new VersionEntry(id, type, url, time, releaseTime));
                }
            } else {
                if (onlyReleases && !"release".equalsIgnoreCase(type)) {
                    continue;
                }
                result.add(new VersionEntry(id, type, url, time, releaseTime));
            }
        }
        return result;
    }

    public Optional<ServerDownload> fetchServerDownload(String versionPackageUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(versionPackageUrl))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch version package: HTTP " + response.statusCode());
        }

        Map<String, Object> pkg = MAPPER.readValue(response.body(), new TypeReference<>() {});
        Object downloadsObj = pkg.get("downloads");
        if (!(downloadsObj instanceof Map<?, ?> downloads)) return Optional.empty();

        Object serverObj = downloads.get("server");
        if (!(serverObj instanceof Map<?, ?> serverMap)) return Optional.empty();

        String url = String.valueOf(serverMap.get("url"));
        String sha1 = String.valueOf(serverMap.get("sha1"));
        long size = serverMap.get("size") instanceof Number num ? num.longValue() : 0L;

        int javaMajor = 21;
        Object javaVersionObj = pkg.get("javaVersion");
        if (javaVersionObj instanceof Map<?, ?> javaMap && javaMap.get("majorVersion") instanceof Number num) {
            javaMajor = num.intValue();
        }

        return Optional.of(new ServerDownload(url, sha1, size, javaMajor));
    }
}
