package com.github.yannicklamprecht.mc.management.console.web.dto;

/**
 * One entry in the {@code GET /api/servers} listing used to populate the server selector, and the
 * response of {@code POST /api/servers/{id}/reconnect}.
 *
 * @param online          whether the console currently has an open MSMP connection to this server
 * @param minecraftVersion the Minecraft version this server reported (e.g. {@code "1.21.11"}),
 *                          only present when {@code online}
 * @param protocolVersion the MSMP protocol version this server's session is speaking
 * @param url             the configured MSMP WebSocket URL for this server, shown in the
 *                         troubleshooting panel when offline
 */
public record ServerSummary(String id, boolean online, String minecraftVersion, String protocolVersion, String url) {
}
