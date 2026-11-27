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
    public static final List<String> SUPPORTED_PROTOCOL_VERSIONS = List.of("1.0.0", "1.1.0", "2.0.0", "3.0.0", "3.1.0");

    public static final String LATEST_PROTOCOL_VERSION = "3.1.0";

    /**
     * Keyed by Minecraft version (e.g. the server status response's {@code version.name}).
     */
    public static final Map<String, String> MINECRAFT_VERSION_TO_PROTOCOL_VERSION;

    static {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("26.4-pre-2", "3.1.0");
        map.put("26.4-pre-1", "3.1.0");
        map.put("26.4-snapshot-10", "3.1.0");
        map.put("26.4-snapshot-9", "3.1.0");
        map.put("26.4-snapshot-8", "3.1.0");
        map.put("26.4-snapshot-7", "3.1.0");
        map.put("26.4-snapshot-6", "3.1.0");
        map.put("26.4-snapshot-5", "3.1.0");
        map.put("26.4-snapshot-4", "3.1.0");
        map.put("26.4-snapshot-3", "3.1.0");
        map.put("26.4-snapshot-2", "3.1.0");
        map.put("26.4-snapshot-1", "3.1.0");
        map.put("26.3", "3.1.0");
        map.put("26.3-rc-3", "3.1.0");
        map.put("26.3-rc-2", "3.1.0");
        map.put("26.3-rc-1", "3.1.0");
        map.put("26.3-pre-3", "3.1.0");
        map.put("26.3-pre-2", "3.1.0");
        map.put("26.3-pre-1", "3.1.0");
        map.put("26.3-snapshot-10", "3.1.0");
        map.put("26.3-snapshot-9", "3.1.0");
        map.put("26.3-snapshot-8", "3.1.0");
        map.put("26.3-snapshot-7", "3.1.0");
        map.put("26.3-snapshot-6", "3.1.0");
        map.put("26.3-snapshot-5", "3.1.0");
        map.put("26.3-snapshot-4", "3.1.0");
        map.put("26.3-snapshot-3", "3.1.0");
        map.put("26.3-snapshot-2", "3.1.0");
        map.put("26.3-snapshot-1", "3.1.0");
        map.put("26.2", "3.0.0");
        map.put("26.2-rc-2", "3.0.0");
        map.put("26.2-rc-1", "3.0.0");
        map.put("26.2-pre-6", "3.0.0");
        map.put("26.2-pre-5", "3.0.0");
        map.put("26.2-pre-4", "3.0.0");
        map.put("26.2-pre-3", "3.0.0");
        map.put("26.2-pre-2", "3.0.0");
        map.put("26.2-pre-1", "3.0.0");
        map.put("26.2-snapshot-8", "3.0.0");
        map.put("26.2-snapshot-7", "3.0.0");
        map.put("26.2-snapshot-6", "3.0.0");
        map.put("26.2-snapshot-5", "3.0.0");
        map.put("26.2-snapshot-4", "2.0.0");
        map.put("26.2-snapshot-3", "2.0.0");
        map.put("26.2-snapshot-2", "2.0.0");
        map.put("26.1.2", "2.0.0");
        map.put("26.1.2-rc-1", "2.0.0");
        map.put("26.2-snapshot-1", "2.0.0");
        map.put("26w14a", "2.0.0");
        map.put("26.1.1", "2.0.0");
        map.put("26.1.1-rc-1", "2.0.0");
        map.put("26.1", "2.0.0");
        map.put("26.1-rc-3", "2.0.0");
        map.put("26.1-rc-2", "2.0.0");
        map.put("26.1-rc-1", "2.0.0");
        map.put("26.1-pre-3", "2.0.0");
        map.put("26.1-pre-2", "2.0.0");
        map.put("26.1-pre-1", "2.0.0");
        map.put("26.1-snapshot-11", "2.0.0");
        map.put("26.1-snapshot-10", "2.0.0");
        map.put("26.1-snapshot-9", "2.0.0");
        map.put("26.1-snapshot-8", "2.0.0");
        map.put("26.1-snapshot-7", "2.0.0");
        map.put("26.1-snapshot-6", "2.0.0");
        map.put("26.1-snapshot-5", "2.0.0");
        map.put("26.1-snapshot-4", "2.0.0");
        map.put("26.1-snapshot-3", "2.0.0");
        map.put("26.1-snapshot-2", "2.0.0");
        map.put("26.1-snapshot-1", "2.0.0");
        map.put("1.21.11", "2.0.0");
        map.put("1.21.11-rc3", "2.0.0");
        map.put("1.21.11-rc2", "2.0.0");
        map.put("1.21.11-rc1", "2.0.0");
        map.put("1.21.11-pre5", "2.0.0");
        map.put("1.21.11-pre4", "2.0.0");
        map.put("1.21.11-pre3", "2.0.0");
        map.put("1.21.11-pre2", "2.0.0");
        map.put("1.21.11-pre1", "2.0.0");
        map.put("25w46a", "2.0.0");
        map.put("25w45a", "2.0.0");
        map.put("25w44a", "2.0.0");
        map.put("25w43a", "1.1.0");
        map.put("25w42a", "1.1.0");
        map.put("25w41a", "1.1.0");
        map.put("1.21.10", "1.0.0");
        map.put("1.21.10-rc1", "1.0.0");
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
