package com.github.yannicklamprecht.mc.management;

import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generated mapping between Minecraft versions and Minecraft Server Management Protocol
 * (MSMP) versions, derived from the schemas extracted by the schema-extractor Gradle
 * plugin. Rerun {@code extractMinecraftManagementSchemas} followed by
 * {@code generateMinecraftManagementSources} to refresh this mapping for new versions.
 */
public final class ProtocolVersions {
    public static final List<String> SUPPORTED_PROTOCOL_VERSIONS = List.of("1.0.0", "2.0.0", "3.0.0", "3.1.0");

    public static final String LATEST_PROTOCOL_VERSION = "3.1.0";

    /**
     * Keyed by Minecraft version (e.g. the server status response's {@code version.name}).
     */
    public static final Map<String, String> MINECRAFT_VERSION_TO_PROTOCOL_VERSION;

    static {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("26.3", "3.1.0");
        map.put("26.2", "3.0.0");
        map.put("26.1.2", "2.0.0");
        map.put("26.1.1", "2.0.0");
        map.put("26.1", "2.0.0");
        map.put("1.21.11", "2.0.0");
        map.put("1.21.10", "1.0.0");
        map.put("1.21.9", "1.0.0");
        MINECRAFT_VERSION_TO_PROTOCOL_VERSION = Collections.unmodifiableMap(map);
    }

    private ProtocolVersions() {
    }

    /**
     * Looks up the MSMP protocol version spoken by a given Minecraft version (e.g. from
     * the server status response's {@code version.name}), or empty if that Minecraft
     * version is not in the generated mapping (predates {@code minMinecraftVersion}, or
     * is newer than the last time the schemas were regenerated).
     */
    public static Optional<String> protocolVersionForMinecraftVersion(String minecraftVersion) {
        return Optional.ofNullable(MINECRAFT_VERSION_TO_PROTOCOL_VERSION.get(minecraftVersion));
    }
}
