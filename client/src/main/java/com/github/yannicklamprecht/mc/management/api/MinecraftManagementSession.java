package com.github.yannicklamprecht.mc.management.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * High-level, version-agnostic abstraction for the Minecraft Server Management Protocol (MSMP).
 * Supports protocol versions 1.0.0, 2.0.0, 3.0.0 and 3.1.0.
 * Select the server protocol with {@code client.session(version)}; no negotiation is performed.
 */
public interface MinecraftManagementSession extends AutoCloseable {

    /**
     * The configured protocol version (e.g. "3.1.0", "1.0.0").
     */
    String protocolVersion();

    // -------------------------------------------------------------
    // Core Server Operations
    // -------------------------------------------------------------

    /**
     * Fetches current server status.
     */
    CompletableFuture<ServerStatusView> getStatus();

    /**
     * Requests the server to save worlds and player data.
     */
    CompletableFuture<Void> save(boolean flush);

    /**
     * Stops the Minecraft server.
     */
    CompletableFuture<Void> stop();

    /**
     * Sends a system chat message to all players on the server.
     */
    CompletableFuture<Void> sendSystemMessage(String message);

    /**
     * Sends a system chat message to a single player, rather than broadcasting to everyone like
     * {@link #sendSystemMessage(String)} does.
     */
    CompletableFuture<Boolean> sendPrivateMessage(String playerNameOrUuid, String message);

    /**
     * Kicks a player from the server.
     */
    CompletableFuture<Void> kickPlayer(String playerNameOrUuid, String reason);

    // -------------------------------------------------------------
    // Collections (Players, Operators, Allowlist, Bans)
    // -------------------------------------------------------------

    CompletableFuture<java.util.List<PlayerView>> getPlayers();

    CompletableFuture<java.util.List<OperatorView>> getOperators();
    CompletableFuture<Void> addOperator(String playerNameOrUuid, long permissionLevel, boolean bypassesPlayerLimit);
    CompletableFuture<Void> removeOperator(String playerNameOrUuid);

    CompletableFuture<java.util.List<PlayerView>> getAllowlist();
    CompletableFuture<Void> addToAllowlist(String playerNameOrUuid);
    CompletableFuture<Void> removeFromAllowlist(String playerNameOrUuid);

    CompletableFuture<java.util.List<UserBanView>> getUserBans();
    CompletableFuture<Void> banUser(String playerNameOrUuid, String reason, String source, String expires);
    CompletableFuture<Void> unbanUser(String playerNameOrUuid);

    CompletableFuture<java.util.List<IpBanView>> getIpBans();
    CompletableFuture<Void> banIp(String ip, String reason, String source, String expires);
    CompletableFuture<Void> unbanIp(String ip);

    // -------------------------------------------------------------
    // Notifications (Version Agnostic)
    // -------------------------------------------------------------

    void onPlayerJoined(Consumer<PlayerView> listener);
    void onPlayerLeft(Consumer<PlayerView> listener);
    void onServerStatus(Consumer<ServerStatusView> listener);
    void onServerStarted(Runnable listener);
    void onServerStopping(Runnable listener);
    void onServerSaved(Runnable listener);
    void onServerSaving(Runnable listener);
    void onServerActivity(Runnable listener);

    CompletableFuture<List<PlayerView>> setAllowlist(List<PlayerView> values);
    CompletableFuture<List<PlayerView>> addAllowlist(List<PlayerView> values);
    CompletableFuture<List<PlayerView>> removeAllowlist(List<PlayerView> values);
    CompletableFuture<List<PlayerView>> clearAllowlist();
    CompletableFuture<List<OperatorView>> setOperators(List<OperatorView> values);
    CompletableFuture<List<OperatorView>> addOperators(List<OperatorView> values);
    CompletableFuture<List<OperatorView>> removeOperators(List<PlayerView> values);
    CompletableFuture<List<OperatorView>> clearOperators();
    CompletableFuture<List<UserBanView>> setUserBans(List<UserBanView> values);
    CompletableFuture<List<UserBanView>> addUserBans(List<UserBanView> values);
    CompletableFuture<List<UserBanView>> removeUserBans(List<PlayerView> values);
    CompletableFuture<List<UserBanView>> clearUserBans();
    CompletableFuture<List<IpBanView>> setIpBans(List<IpBanView> values);
    CompletableFuture<List<IpBanView>> addIpBans(List<IncomingIpBanView> values);
    CompletableFuture<List<IpBanView>> removeIpBans(List<String> values);
    CompletableFuture<List<IpBanView>> clearIpBans();
    CompletableFuture<List<PlayerView>> kickPlayers(List<KickPlayerView> players);
    CompletableFuture<Boolean> sendSystemMessage(SystemMessageView message);
    CompletableFuture<Boolean> getAutosave();
    CompletableFuture<Boolean> setAutosave(Boolean value);
    CompletableFuture<Difficulty> getDifficulty();
    CompletableFuture<Difficulty> setDifficulty(Difficulty value);
    CompletableFuture<Boolean> getEnforceAllowlist();
    CompletableFuture<Boolean> setEnforceAllowlist(Boolean value);
    CompletableFuture<Boolean> getUseAllowlist();
    CompletableFuture<Boolean> setUseAllowlist(Boolean value);
    CompletableFuture<Long> getMaxPlayers();
    CompletableFuture<Long> setMaxPlayers(Long value);
    CompletableFuture<Long> getPauseWhenEmptySeconds();
    CompletableFuture<Long> setPauseWhenEmptySeconds(Long value);
    CompletableFuture<Long> getPlayerIdleTimeout();
    CompletableFuture<Long> setPlayerIdleTimeout(Long value);
    CompletableFuture<Boolean> getAllowFlight();
    CompletableFuture<Boolean> setAllowFlight(Boolean value);
    CompletableFuture<String> getMotd();
    CompletableFuture<String> setMotd(String value);
    CompletableFuture<Long> getSpawnProtectionRadius();
    CompletableFuture<Long> setSpawnProtectionRadius(Long value);
    CompletableFuture<Boolean> getForceGameMode();
    CompletableFuture<Boolean> setForceGameMode(Boolean value);
    CompletableFuture<GameMode> getGameMode();
    CompletableFuture<GameMode> setGameMode(GameMode value);
    CompletableFuture<Long> getViewDistance();
    CompletableFuture<Long> setViewDistance(Long value);
    CompletableFuture<Long> getSimulationDistance();
    CompletableFuture<Long> setSimulationDistance(Long value);
    CompletableFuture<Boolean> getAcceptTransfers();
    CompletableFuture<Boolean> setAcceptTransfers(Boolean value);
    CompletableFuture<Long> getStatusHeartbeatInterval();
    CompletableFuture<Long> setStatusHeartbeatInterval(Long value);
    CompletableFuture<Long> getOperatorUserPermissionLevel();
    CompletableFuture<Long> setOperatorUserPermissionLevel(Long value);
    CompletableFuture<Boolean> getHideOnlinePlayers();
    CompletableFuture<Boolean> setHideOnlinePlayers(Boolean value);
    CompletableFuture<Boolean> getStatusReplies();
    CompletableFuture<Boolean> setStatusReplies(Boolean value);
    CompletableFuture<Long> getEntityBroadcastRange();
    CompletableFuture<Long> setEntityBroadcastRange(Long value);
    void onOperatorAdded(Consumer<OperatorView> listener);
    void onOperatorRemoved(Consumer<OperatorView> listener);
    void onAllowlistAdded(Consumer<PlayerView> listener);
    void onAllowlistRemoved(Consumer<PlayerView> listener);
    void onIpBanAdded(Consumer<IpBanView> listener);
    void onIpBanRemoved(Consumer<String> listener);
    void onUserBanAdded(Consumer<UserBanView> listener);
    void onUserBanRemoved(Consumer<PlayerView> listener);
    void onGameRuleUpdated(Consumer<GameRuleView> listener);
    /** Requires protocol 3.1.0. */
    void onWorldUpgradeStarted(Runnable listener);
    /** Requires protocol 3.1.0. */
    void onWorldUpgradeProgress(Consumer<BigDecimal> listener);
    /** Requires protocol 3.1.0. */
    void onWorldUpgradeFinished(Runnable listener);
    /** Requires protocol 3.1.0. */
    void onWorldUpgradeFailed(Consumer<String> listener);

    CompletableFuture<List<GameRuleView>> getGameRules();
    CompletableFuture<GameRuleView> updateGameRule(String key, boolean value);
    CompletableFuture<GameRuleView> updateGameRule(String key, long value);

    // -------------------------------------------------------------
    // Views (Unified Data Records)
    // -------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PlayerView(String id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record VersionView(String name, Long protocol) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ServerStatusView(boolean started, java.util.List<PlayerView> players, VersionView version) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OperatorView(boolean bypassesPlayerLimit, long permissionLevel, PlayerView player) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record UserBanView(String created, String expires, String reason, String source, PlayerView player) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IpBanView(String ip, String created, String expires, String reason, String source) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IncomingIpBanView(String ip, PlayerView player, String reason, String source, String expires) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record MessageView(String literal, String translatable, List<String> translatableParams) {
        public static MessageView literal(String text) {
            return new MessageView(java.util.Objects.requireNonNull(text), null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record KickPlayerView(PlayerView player, MessageView message) {}

    /**
     * {@code receivingPlayers} must be {@code null} (omitted from the wire payload, thanks to
     * {@code @JsonInclude(NON_NULL)}) to broadcast to every player. An explicit empty list is a
     * distinct value on the wire - "send to these zero players" - and reaches nobody.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SystemMessageView(MessageView message, boolean overlay, List<PlayerView> receivingPlayers) {}

    /** Values are normalized to Boolean or Long, including protocol 1.0.0 string values. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GameRuleView(String key, String type, Object value) {
        public GameRuleView {
            if ("boolean".equals(type)) {
                if (!(value instanceof Boolean)) {
                    if (!"true".equals(value) && !"false".equals(value)) {
                        throw new IllegalArgumentException("Invalid boolean game-rule value: " + value);
                    }
                    value = Boolean.valueOf(value.toString());
                }
            } else if ("integer".equals(type)) {
                value = Long.valueOf(value.toString());
            } else {
                throw new IllegalArgumentException("Unknown game-rule type: " + type);
            }
        }
    }

    enum Difficulty {
        @JsonProperty("peaceful") PEACEFUL,
        @JsonProperty("easy") EASY,
        @JsonProperty("normal") NORMAL,
        @JsonProperty("hard") HARD
    }

    enum GameMode {
        @JsonProperty("survival") SURVIVAL,
        @JsonProperty("creative") CREATIVE,
        @JsonProperty("adventure") ADVENTURE,
        @JsonProperty("spectator") SPECTATOR
    }
}
