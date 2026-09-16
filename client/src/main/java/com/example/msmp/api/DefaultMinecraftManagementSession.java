package com.example.msmp.api;

import com.example.msmp.transport.MinecraftManagementClient;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class DefaultMinecraftManagementSession implements MinecraftManagementSession {
    private final MinecraftManagementClient client;
    private final String protocolVersion;

    public DefaultMinecraftManagementSession(MinecraftManagementClient client, String protocolVersion) {
        this.client = Objects.requireNonNull(client, "client");
        this.protocolVersion = protocolVersion != null && !protocolVersion.isBlank() ? protocolVersion : "3.1.0";
    }

    @Override
    public String protocolVersion() {
        return protocolVersion;
    }

    @Override
    public CompletableFuture<ServerStatusView> getStatus() {
        return client.call("minecraft:server/status", new TypeReference<Map<String, Object>>() {})
                .thenApply(this::mapServerStatus);
    }

    @Override
    public CompletableFuture<Void> save(boolean flush) {
        return client.call("minecraft:server/save", Map.of("flush", flush), new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<Void> stop() {
        return client.call("minecraft:server/stop", new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<Void> sendSystemMessage(String message) {
        return client.call("minecraft:server/system_message", Map.of("message", message), new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<Void> kickPlayer(String playerNameOrUuid, String reason) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        Map<String, Object> params = reason != null && !reason.isBlank()
                ? Map.of("player", playerMap, "message", Map.of("literal", reason))
                : Map.of("player", playerMap);
        return client.call("minecraft:players/kick", params, new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<List<PlayerView>> getPlayers() {
        return client.call("minecraft:players", new TypeReference<List<Map<String, Object>>>() {})
                .thenApply(list -> list.stream().map(this::mapPlayer).toList());
    }

    @Override
    public CompletableFuture<List<OperatorView>> getOperators() {
        return client.call("minecraft:operators", new TypeReference<List<Map<String, Object>>>() {})
                .thenApply(list -> list.stream().map(this::mapOperator).toList());
    }

    @Override
    public CompletableFuture<Void> addOperator(String playerNameOrUuid, long permissionLevel, boolean bypassesPlayerLimit) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        Map<String, Object> operatorMap = Map.of(
                "player", playerMap,
                "permissionLevel", permissionLevel,
                "bypassesPlayerLimit", bypassesPlayerLimit
        );
        return client.call("minecraft:operators/add", Map.of("operator", operatorMap), new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<Void> removeOperator(String playerNameOrUuid) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        return client.call("minecraft:operators/remove", Map.of("player", playerMap), new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<List<PlayerView>> getAllowlist() {
        return client.call("minecraft:allowlist", new TypeReference<List<Map<String, Object>>>() {})
                .thenApply(list -> list.stream().map(this::mapPlayer).toList());
    }

    @Override
    public CompletableFuture<Void> addToAllowlist(String playerNameOrUuid) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        return client.call("minecraft:allowlist/add", Map.of("player", playerMap), new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<Void> removeFromAllowlist(String playerNameOrUuid) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        return client.call("minecraft:allowlist/remove", Map.of("player", playerMap), new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<List<UserBanView>> getUserBans() {
        return client.call("minecraft:bans", new TypeReference<List<Map<String, Object>>>() {})
                .thenApply(list -> list.stream().map(this::mapUserBan).toList());
    }

    @Override
    public CompletableFuture<Void> banUser(String playerNameOrUuid, String reason, String source, String expires) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        Map<String, Object> banMap = new java.util.HashMap<>();
        banMap.put("player", playerMap);
        if (reason != null) banMap.put("reason", reason);
        if (source != null) banMap.put("source", source);
        if (expires != null) banMap.put("expires", expires);

        return client.call("minecraft:bans/add", Map.of("ban", banMap), new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<Void> unbanUser(String playerNameOrUuid) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        return client.call("minecraft:bans/remove", Map.of("player", playerMap), new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<List<IpBanView>> getIpBans() {
        return client.call("minecraft:ip_bans", new TypeReference<List<Map<String, Object>>>() {})
                .thenApply(list -> list.stream().map(this::mapIpBan).toList());
    }

    @Override
    public CompletableFuture<Void> banIp(String ip, String reason, String source, String expires) {
        Map<String, Object> banMap = new java.util.HashMap<>();
        banMap.put("ip", ip);
        if (reason != null) banMap.put("reason", reason);
        if (source != null) banMap.put("source", source);
        if (expires != null) banMap.put("expires", expires);

        return client.call("minecraft:ip_bans/add", Map.of("ban", banMap), new TypeReference<Void>() {});
    }

    @Override
    public CompletableFuture<Void> unbanIp(String ip) {
        return client.call("minecraft:ip_bans/remove", Map.of("ip", ip), new TypeReference<Void>() {});
    }

    @Override
    public void onPlayerJoined(Consumer<PlayerView> listener) {
        client.registerNotificationProperty("minecraft:notification/players/joined", "player",
                new TypeReference<Map<String, Object>>() {},
                raw -> listener.accept(mapPlayer(raw)));
    }

    @Override
    public void onPlayerLeft(Consumer<PlayerView> listener) {
        client.registerNotificationProperty("minecraft:notification/players/left", "player",
                new TypeReference<Map<String, Object>>() {},
                raw -> listener.accept(mapPlayer(raw)));
    }

    @Override
    public void onServerStatus(Consumer<ServerStatusView> listener) {
        client.registerNotificationProperty("minecraft:notification/server/status", "status",
                new TypeReference<Map<String, Object>>() {},
                raw -> listener.accept(mapServerStatus(raw)));
    }

    @Override
    public void onServerStarted(Runnable listener) {
        client.registerNotification("minecraft:notification/server/started", listener);
    }

    @Override
    public void onServerStopping(Runnable listener) {
        client.registerNotification("minecraft:notification/server/stopping", listener);
    }

    @Override
    public void onServerSaved(Runnable listener) {
        client.registerNotification("minecraft:notification/server/saved", listener);
    }

    @Override
    public void onServerSaving(Runnable listener) {
        client.registerNotification("minecraft:notification/server/saving", listener);
    }

    @Override
    public void onServerActivity(Runnable listener) {
        client.registerNotification("minecraft:notification/server/activity", listener);
    }

    @Override
    public void close() {
        client.close();
    }

    private PlayerView mapPlayer(Map<String, Object> m) {
        if (m == null) return null;
        return new PlayerView(
                str(m.get("id")),
                str(m.get("name"))
        );
    }

    private VersionView mapVersion(Map<String, Object> m) {
        if (m == null) return null;
        Long protocol = m.get("protocol") instanceof Number n ? n.longValue() : null;
        return new VersionView(str(m.get("name")), protocol);
    }

    @SuppressWarnings("unchecked")
    private ServerStatusView mapServerStatus(Map<String, Object> m) {
        if (m == null) return null;
        boolean started = Boolean.TRUE.equals(m.get("started"));
        List<PlayerView> players = List.of();
        if (m.get("players") instanceof List<?> list) {
            players = list.stream()
                    .filter(Map.class::isInstance)
                    .map(o -> mapPlayer((Map<String, Object>) o))
                    .toList();
        }
        VersionView version = null;
        if (m.get("version") instanceof Map<?, ?> vMap) {
            version = mapVersion((Map<String, Object>) vMap);
        }
        return new ServerStatusView(started, players, version);
    }

    private OperatorView mapOperator(Map<String, Object> m) {
        if (m == null) return null;
        boolean bypasses = Boolean.TRUE.equals(m.get("bypassesPlayerLimit"));
        long level = m.get("permissionLevel") instanceof Number n ? n.longValue() : 0L;
        PlayerView player = m.get("player") instanceof Map<?, ?> pMap ? mapPlayer((Map<String, Object>) pMap) : null;
        return new OperatorView(bypasses, level, player);
    }

    private UserBanView mapUserBan(Map<String, Object> m) {
        if (m == null) return null;
        PlayerView player = m.get("player") instanceof Map<?, ?> pMap ? mapPlayer((Map<String, Object>) pMap) : null;
        return new UserBanView(
                str(m.get("created")),
                str(m.get("expires")),
                str(m.get("reason")),
                str(m.get("source")),
                player
        );
    }

    private IpBanView mapIpBan(Map<String, Object> m) {
        if (m == null) return null;
        return new IpBanView(
                str(m.get("ip")),
                str(m.get("created")),
                str(m.get("expires")),
                str(m.get("reason")),
                str(m.get("source"))
        );
    }

    private static boolean isUuid(String s) {
        if (s == null) return false;
        return s.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
