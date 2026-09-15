package com.example.msmp.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * High-level, version-agnostic abstraction for the Minecraft Server Management Protocol (MSMP).
 * Automatically handles protocol differences across versions 1.0.0 through 3.1.0+.
 */
public interface MinecraftManagementSession extends AutoCloseable {

    /**
     * The negotiated or detected protocol version (e.g. "3.1.0", "1.0.0").
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

    // -------------------------------------------------------------
    // Views (Unified Data Records)
    // -------------------------------------------------------------

    record PlayerView(String id, String name) {}

    record VersionView(String name, Long protocol) {}

    record ServerStatusView(boolean started, java.util.List<PlayerView> players, VersionView version) {}

    record OperatorView(boolean bypassesPlayerLimit, long permissionLevel, PlayerView player) {}

    record UserBanView(String created, String expires, String reason, String source, PlayerView player) {}

    record IpBanView(String ip, String created, String expires, String reason, String source) {}
}
