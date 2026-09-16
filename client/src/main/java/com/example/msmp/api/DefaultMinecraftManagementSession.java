package com.example.msmp.api;

import com.example.msmp.transport.MinecraftManagementClient;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.math.BigDecimal;
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
        if (!List.of("1.0.0", "2.0.0", "3.0.0", "3.1.0").contains(this.protocolVersion)) {
            throw new IllegalArgumentException("Unsupported protocol version: " + this.protocolVersion);
        }
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
        return client.call("minecraft:server/save", Map.of("flush", flush), new TypeReference<Boolean>() {})
                .thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Void> stop() {
        return client.call("minecraft:server/stop", new TypeReference<Boolean>() {})
                .thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Void> sendSystemMessage(String message) {
        return sendSystemMessage(new SystemMessageView(MessageView.literal(message), false, List.of()))
                .thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Void> kickPlayer(String playerNameOrUuid, String reason) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        Map<String, Object> params = reason != null && !reason.isBlank()
                ? Map.of("player", playerMap, "message", Map.of("literal", reason))
                : Map.of("player", playerMap);
        return client.call("minecraft:players/kick", Map.of("kick", List.of(params)), new TypeReference<List<PlayerView>>() {})
                .thenApply(ignored -> null);
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
        return client.call("minecraft:operators/add", Map.of("add", List.of(operatorMap)), new TypeReference<List<OperatorView>>() {})
                .thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Void> removeOperator(String playerNameOrUuid) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        return client.call("minecraft:operators/remove", Map.of("remove", List.of(playerMap)), new TypeReference<List<OperatorView>>() {})
                .thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<List<PlayerView>> getAllowlist() {
        return client.call("minecraft:allowlist", new TypeReference<List<Map<String, Object>>>() {})
                .thenApply(list -> list.stream().map(this::mapPlayer).toList());
    }

    @Override
    public CompletableFuture<Void> addToAllowlist(String playerNameOrUuid) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        return client.call("minecraft:allowlist/add", Map.of("add", List.of(playerMap)), new TypeReference<List<PlayerView>>() {})
                .thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Void> removeFromAllowlist(String playerNameOrUuid) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        return client.call("minecraft:allowlist/remove", Map.of("remove", List.of(playerMap)), new TypeReference<List<PlayerView>>() {})
                .thenApply(ignored -> null);
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

        return client.call("minecraft:bans/add", Map.of("add", List.of(banMap)), new TypeReference<List<UserBanView>>() {})
                .thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Void> unbanUser(String playerNameOrUuid) {
        Map<String, Object> playerMap = isUuid(playerNameOrUuid) ? Map.of("id", playerNameOrUuid) : Map.of("name", playerNameOrUuid);
        return client.call("minecraft:bans/remove", Map.of("remove", List.of(playerMap)), new TypeReference<List<UserBanView>>() {})
                .thenApply(ignored -> null);
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

        return client.call("minecraft:ip_bans/add", Map.of("add", List.of(banMap)), new TypeReference<List<IpBanView>>() {})
                .thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Void> unbanIp(String ip) {
        return client.call("minecraft:ip_bans/remove", Map.of("ip", List.of(ip)), new TypeReference<List<IpBanView>>() {})
                .thenApply(ignored -> null);
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
        if (protocolVersion.equals("1.0.0")) {
            throw new UnsupportedOperationException("Server activity notifications require protocol 2.0.0 or later");
        }
        client.registerNotification("minecraft:notification/server/activity", listener);
    }

    @Override
    public CompletableFuture<List<PlayerView>> setAllowlist(List<PlayerView> values) {
        return client.call("minecraft:allowlist/set", Map.of("players", values), new TypeReference<List<PlayerView>>() {});
    }

    @Override
    public CompletableFuture<List<PlayerView>> addAllowlist(List<PlayerView> values) {
        return client.call("minecraft:allowlist/add", Map.of("add", values), new TypeReference<List<PlayerView>>() {});
    }

    @Override
    public CompletableFuture<List<PlayerView>> removeAllowlist(List<PlayerView> values) {
        return client.call("minecraft:allowlist/remove", Map.of("remove", values), new TypeReference<List<PlayerView>>() {});
    }

    @Override
    public CompletableFuture<List<PlayerView>> clearAllowlist() {
        return client.call("minecraft:allowlist/clear", new TypeReference<List<PlayerView>>() {});
    }

    @Override
    public CompletableFuture<List<OperatorView>> setOperators(List<OperatorView> values) {
        return client.call("minecraft:operators/set", Map.of("operators", values), new TypeReference<List<OperatorView>>() {});
    }

    @Override
    public CompletableFuture<List<OperatorView>> addOperators(List<OperatorView> values) {
        return client.call("minecraft:operators/add", Map.of("add", values), new TypeReference<List<OperatorView>>() {});
    }

    @Override
    public CompletableFuture<List<OperatorView>> removeOperators(List<PlayerView> values) {
        return client.call("minecraft:operators/remove", Map.of("remove", values), new TypeReference<List<OperatorView>>() {});
    }

    @Override
    public CompletableFuture<List<OperatorView>> clearOperators() {
        return client.call("minecraft:operators/clear", new TypeReference<List<OperatorView>>() {});
    }

    @Override
    public CompletableFuture<List<UserBanView>> setUserBans(List<UserBanView> values) {
        return client.call("minecraft:bans/set", Map.of("bans", values), new TypeReference<List<UserBanView>>() {});
    }

    @Override
    public CompletableFuture<List<UserBanView>> addUserBans(List<UserBanView> values) {
        return client.call("minecraft:bans/add", Map.of("add", values), new TypeReference<List<UserBanView>>() {});
    }

    @Override
    public CompletableFuture<List<UserBanView>> removeUserBans(List<PlayerView> values) {
        return client.call("minecraft:bans/remove", Map.of("remove", values), new TypeReference<List<UserBanView>>() {});
    }

    @Override
    public CompletableFuture<List<UserBanView>> clearUserBans() {
        return client.call("minecraft:bans/clear", new TypeReference<List<UserBanView>>() {});
    }

    @Override
    public CompletableFuture<List<IpBanView>> setIpBans(List<IpBanView> values) {
        return client.call("minecraft:ip_bans/set", Map.of("banlist", values), new TypeReference<List<IpBanView>>() {});
    }

    @Override
    public CompletableFuture<List<IpBanView>> addIpBans(List<IncomingIpBanView> values) {
        return client.call("minecraft:ip_bans/add", Map.of("add", values), new TypeReference<List<IpBanView>>() {});
    }

    @Override
    public CompletableFuture<List<IpBanView>> removeIpBans(List<String> values) {
        return client.call("minecraft:ip_bans/remove", Map.of("ip", values), new TypeReference<List<IpBanView>>() {});
    }

    @Override
    public CompletableFuture<List<IpBanView>> clearIpBans() {
        return client.call("minecraft:ip_bans/clear", new TypeReference<List<IpBanView>>() {});
    }

    @Override
    public CompletableFuture<List<PlayerView>> kickPlayers(List<KickPlayerView> players) {
        return client.call("minecraft:players/kick", Map.of("kick", players), new TypeReference<List<PlayerView>>() {});
    }

    @Override
    public CompletableFuture<Boolean> sendSystemMessage(SystemMessageView message) {
        return client.call("minecraft:server/system_message", Map.of("message", message), new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> getAutosave() {
        return client.call("minecraft:serversettings/autosave", new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> setAutosave(Boolean value) {
        return client.call("minecraft:serversettings/autosave/set", Map.of("enable", value), new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Difficulty> getDifficulty() {
        return client.call("minecraft:serversettings/difficulty", new TypeReference<Difficulty>() {});
    }

    @Override
    public CompletableFuture<Difficulty> setDifficulty(Difficulty value) {
        return client.call("minecraft:serversettings/difficulty/set", Map.of("difficulty", value), new TypeReference<Difficulty>() {});
    }

    @Override
    public CompletableFuture<Boolean> getEnforceAllowlist() {
        return client.call("minecraft:serversettings/enforce_allowlist", new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> setEnforceAllowlist(Boolean value) {
        return client.call("minecraft:serversettings/enforce_allowlist/set", Map.of("enforce", value), new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> getUseAllowlist() {
        return client.call("minecraft:serversettings/use_allowlist", new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> setUseAllowlist(Boolean value) {
        return client.call("minecraft:serversettings/use_allowlist/set", Map.of("use", value), new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Long> getMaxPlayers() {
        return client.call("minecraft:serversettings/max_players", new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> setMaxPlayers(Long value) {
        return client.call("minecraft:serversettings/max_players/set", Map.of("max", value), new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> getPauseWhenEmptySeconds() {
        return client.call("minecraft:serversettings/pause_when_empty_seconds", new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> setPauseWhenEmptySeconds(Long value) {
        return client.call("minecraft:serversettings/pause_when_empty_seconds/set", Map.of("seconds", value), new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> getPlayerIdleTimeout() {
        return client.call("minecraft:serversettings/player_idle_timeout", new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> setPlayerIdleTimeout(Long value) {
        return client.call("minecraft:serversettings/player_idle_timeout/set", Map.of("seconds", value), new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Boolean> getAllowFlight() {
        return client.call("minecraft:serversettings/allow_flight", new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> setAllowFlight(Boolean value) {
        return client.call("minecraft:serversettings/allow_flight/set", Map.of("allow", value), new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<String> getMotd() {
        return client.call("minecraft:serversettings/motd", new TypeReference<String>() {});
    }

    @Override
    public CompletableFuture<String> setMotd(String value) {
        return client.call("minecraft:serversettings/motd/set", Map.of("message", value), new TypeReference<String>() {});
    }

    @Override
    public CompletableFuture<Long> getSpawnProtectionRadius() {
        return client.call("minecraft:serversettings/spawn_protection_radius", new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> setSpawnProtectionRadius(Long value) {
        return client.call("minecraft:serversettings/spawn_protection_radius/set", Map.of("radius", value), new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Boolean> getForceGameMode() {
        return client.call("minecraft:serversettings/force_game_mode", new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> setForceGameMode(Boolean value) {
        return client.call("minecraft:serversettings/force_game_mode/set", Map.of("force", value), new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<GameMode> getGameMode() {
        return client.call("minecraft:serversettings/game_mode", new TypeReference<GameMode>() {});
    }

    @Override
    public CompletableFuture<GameMode> setGameMode(GameMode value) {
        return client.call("minecraft:serversettings/game_mode/set", Map.of("mode", value), new TypeReference<GameMode>() {});
    }

    @Override
    public CompletableFuture<Long> getViewDistance() {
        return client.call("minecraft:serversettings/view_distance", new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> setViewDistance(Long value) {
        return client.call("minecraft:serversettings/view_distance/set", Map.of("distance", value), new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> getSimulationDistance() {
        return client.call("minecraft:serversettings/simulation_distance", new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> setSimulationDistance(Long value) {
        return client.call("minecraft:serversettings/simulation_distance/set", Map.of("distance", value), new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Boolean> getAcceptTransfers() {
        return client.call("minecraft:serversettings/accept_transfers", new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> setAcceptTransfers(Boolean value) {
        return client.call("minecraft:serversettings/accept_transfers/set", Map.of("accept", value), new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Long> getStatusHeartbeatInterval() {
        return client.call("minecraft:serversettings/status_heartbeat_interval", new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> setStatusHeartbeatInterval(Long value) {
        return client.call("minecraft:serversettings/status_heartbeat_interval/set", Map.of("seconds", value), new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> getOperatorUserPermissionLevel() {
        return client.call("minecraft:serversettings/operator_user_permission_level", new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> setOperatorUserPermissionLevel(Long value) {
        return client.call("minecraft:serversettings/operator_user_permission_level/set", Map.of("level", value), new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Boolean> getHideOnlinePlayers() {
        return client.call("minecraft:serversettings/hide_online_players", new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> setHideOnlinePlayers(Boolean value) {
        return client.call("minecraft:serversettings/hide_online_players/set", Map.of("hide", value), new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> getStatusReplies() {
        return client.call("minecraft:serversettings/status_replies", new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Boolean> setStatusReplies(Boolean value) {
        return client.call("minecraft:serversettings/status_replies/set", Map.of("enable", value), new TypeReference<Boolean>() {});
    }

    @Override
    public CompletableFuture<Long> getEntityBroadcastRange() {
        return client.call("minecraft:serversettings/entity_broadcast_range", new TypeReference<Long>() {});
    }

    @Override
    public CompletableFuture<Long> setEntityBroadcastRange(Long value) {
        return client.call("minecraft:serversettings/entity_broadcast_range/set", Map.of("percentage_points", value), new TypeReference<Long>() {});
    }

    @Override
    public void onOperatorAdded(Consumer<OperatorView> listener) {
        client.registerNotificationProperty("minecraft:notification/operators/added", "player", new TypeReference<OperatorView>() {}, listener);
    }

    @Override
    public void onOperatorRemoved(Consumer<OperatorView> listener) {
        client.registerNotificationProperty("minecraft:notification/operators/removed", "player", new TypeReference<OperatorView>() {}, listener);
    }

    @Override
    public void onAllowlistAdded(Consumer<PlayerView> listener) {
        client.registerNotificationProperty("minecraft:notification/allowlist/added", "player", new TypeReference<PlayerView>() {}, listener);
    }

    @Override
    public void onAllowlistRemoved(Consumer<PlayerView> listener) {
        client.registerNotificationProperty("minecraft:notification/allowlist/removed", "player", new TypeReference<PlayerView>() {}, listener);
    }

    @Override
    public void onIpBanAdded(Consumer<IpBanView> listener) {
        client.registerNotificationProperty("minecraft:notification/ip_bans/added", "player", new TypeReference<IpBanView>() {}, listener);
    }

    @Override
    public void onIpBanRemoved(Consumer<String> listener) {
        client.registerNotificationProperty("minecraft:notification/ip_bans/removed", "player", new TypeReference<String>() {}, listener);
    }

    @Override
    public void onUserBanAdded(Consumer<UserBanView> listener) {
        client.registerNotificationProperty("minecraft:notification/bans/added", "player", new TypeReference<UserBanView>() {}, listener);
    }

    @Override
    public void onUserBanRemoved(Consumer<PlayerView> listener) {
        client.registerNotificationProperty("minecraft:notification/bans/removed", "player", new TypeReference<PlayerView>() {}, listener);
    }

    @Override
    public void onGameRuleUpdated(Consumer<GameRuleView> listener) {
        client.registerNotificationProperty("minecraft:notification/gamerules/updated", "gamerule", new TypeReference<GameRuleView>() {}, listener);
    }

    @Override
    public void onWorldUpgradeStarted(Runnable listener) {
        requireWorldUpgradeNotifications();
        client.registerNotification("minecraft:notification/world/upgrade_started", listener);
    }

    @Override
    public void onWorldUpgradeProgress(Consumer<BigDecimal> listener) {
        requireWorldUpgradeNotifications();
        client.registerNotificationProperty("minecraft:notification/world/upgrade_progress", "progress", new TypeReference<BigDecimal>() {}, listener);
    }

    @Override
    public void onWorldUpgradeFinished(Runnable listener) {
        requireWorldUpgradeNotifications();
        client.registerNotification("minecraft:notification/world/upgrade_finished", listener);
    }

    @Override
    public void onWorldUpgradeFailed(Consumer<String> listener) {
        requireWorldUpgradeNotifications();
        client.registerNotificationProperty("minecraft:notification/world/upgrade_failed", "reason", new TypeReference<String>() {}, listener);
    }
    @Override
    public CompletableFuture<List<GameRuleView>> getGameRules() {
        return client.call("minecraft:gamerules", new TypeReference<List<GameRuleView>>() {});
    }

    @Override
    public CompletableFuture<GameRuleView> updateGameRule(String key, boolean value) {
        return updateGameRuleValue(key, value);
    }

    @Override
    public CompletableFuture<GameRuleView> updateGameRule(String key, long value) {
        return updateGameRuleValue(key, value);
    }

    private CompletableFuture<GameRuleView> updateGameRuleValue(String key, Object value) {
        Object encodedValue = protocolVersion.equals("1.0.0") ? value.toString() : value;
        return client.call("minecraft:gamerules/update",
                Map.of("gamerule", Map.of("key", key, "value", encodedValue)),
                new TypeReference<GameRuleView>() {});
    }

    private void requireWorldUpgradeNotifications() {
        if (!protocolVersion.equals("3.1.0")) {
            throw new UnsupportedOperationException("World upgrade notifications require protocol 3.1.0");
        }
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
