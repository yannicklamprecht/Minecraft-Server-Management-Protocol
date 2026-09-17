package com.github.yannicklamprecht.mc.management.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration attributes for connecting to one or more Minecraft Server Management Protocol
 * (MSMP) servers.
 * <p>
 * Every server is configured by name under {@code minecraft.management.servers.<id>.*}, e.g.
 * <pre>{@code
 * minecraft:
 *   management:
 *     servers:
 *       survival:
 *         url: ws://localhost:25585
 *         secret: ${SURVIVAL_SECRET}
 *       creative:
 *         url: ws://localhost:25586
 *         secret: ${CREATIVE_SECRET}
 * }</pre>
 */
@ConfigurationProperties(prefix = "minecraft.management")
public class MinecraftManagementProperties {

    /**
     * Named MSMP servers to connect to, keyed by a short id used throughout the API
     * (e.g. {@code /api/servers/{id}/...}) and on every published notification event.
     */
    private Map<String, ServerProperties> servers = new LinkedHashMap<>();

    public Map<String, ServerProperties> getServers() {
        return servers;
    }

    public void setServers(Map<String, ServerProperties> servers) {
        this.servers = servers;
    }

    /** Connection attributes for a single named MSMP server. */
    public static class ServerProperties {

        private URI url = URI.create("ws://localhost:25585");
        private String secret;
        /**
         * The MSMP protocol version to speak. Left {@code null} by default: the server's actual
         * protocol version is then auto-detected right after connecting (see
         * {@code MinecraftManagementClient#detectProtocolVersion()}) instead of being assumed. Set
         * this explicitly to skip that detection round-trip or to pin a specific version.
         */
        private String protocolVersion;
        private boolean autoConnect = true;

        public URI getUrl() {
            return url;
        }

        public void setUrl(URI url) {
            this.url = url;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getProtocolVersion() {
            return protocolVersion;
        }

        public void setProtocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        /** {@code true} when no explicit {@code protocol-version} was configured. */
        public boolean isProtocolVersionAutoDetected() {
            return protocolVersion == null || protocolVersion.isBlank();
        }

        public boolean isAutoConnect() {
            return autoConnect;
        }

        public void setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
        }
    }
}
