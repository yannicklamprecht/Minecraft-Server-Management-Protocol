package com.github.yannicklamprecht.mc.management.v2_0_0;

import com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.Difficulty;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.GameType;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.IncomingIpBan;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.IpBan;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.KickPlayer;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.Operator;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.Player;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.ServerState;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.SystemMessage;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.TypedGameRule;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.UntypedGameRule;
import com.github.yannicklamprecht.mc.management.v2_0_0.dto.UserBan;
import java.lang.Boolean;
import java.lang.Long;
import java.lang.String;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import tools.jackson.core.type.TypeReference;

/**
 * Typed JSON-RPC API facade for Minecraft Management Protocol v2.0.0.
 */
public final class MinecraftManagementApi {
    private final MinecraftManagementClient client;

    public MinecraftManagementApi(MinecraftManagementClient client) {
        this.client = client;
    }

    public MinecraftManagementClient client() {
        return client;
    }

    /**
     * Get the allowlist
     */
    public CompletableFuture<List<Player>> allowlist() {
        return client.call("minecraft:allowlist", new TypeReference<List<Player>>() {});
    }

    /**
     * Set the allowlist
     */
    public CompletableFuture<List<Player>> allowlistSet(List<Player> players) {
        return client.call("minecraft:allowlist/set", Map.of("players", players), new TypeReference<List<Player>>() {});
    }

    /**
     * Add players to allowlist
     */
    public CompletableFuture<List<Player>> allowlistAdd(List<Player> add) {
        return client.call("minecraft:allowlist/add", Map.of("add", add), new TypeReference<List<Player>>() {});
    }

    /**
     * Remove players from allowlist
     */
    public CompletableFuture<List<Player>> allowlistRemove(List<Player> remove) {
        return client.call("minecraft:allowlist/remove", Map.of("remove", remove), new TypeReference<List<Player>>() {});
    }

    /**
     * Clear all players in allowlist
     */
    public CompletableFuture<List<Player>> allowlistClear() {
        return client.call("minecraft:allowlist/clear", new TypeReference<List<Player>>() {});
    }

    /**
     * Get the ban list
     */
    public CompletableFuture<List<UserBan>> bans() {
        return client.call("minecraft:bans", new TypeReference<List<UserBan>>() {});
    }

    /**
     * Set the banlist
     */
    public CompletableFuture<List<UserBan>> bansSet(List<UserBan> bans) {
        return client.call("minecraft:bans/set", Map.of("bans", bans), new TypeReference<List<UserBan>>() {});
    }

    /**
     * Add players to ban list
     */
    public CompletableFuture<List<UserBan>> bansAdd(List<UserBan> add) {
        return client.call("minecraft:bans/add", Map.of("add", add), new TypeReference<List<UserBan>>() {});
    }

    /**
     * Remove players from ban list
     */
    public CompletableFuture<List<UserBan>> bansRemove(List<Player> remove) {
        return client.call("minecraft:bans/remove", Map.of("remove", remove), new TypeReference<List<UserBan>>() {});
    }

    /**
     * Clear all players in ban list
     */
    public CompletableFuture<List<UserBan>> bansClear() {
        return client.call("minecraft:bans/clear", new TypeReference<List<UserBan>>() {});
    }

    /**
     * Get the ip ban list
     */
    public CompletableFuture<List<IpBan>> ipBans() {
        return client.call("minecraft:ip_bans", new TypeReference<List<IpBan>>() {});
    }

    /**
     * Set the ip banlist
     */
    public CompletableFuture<List<IpBan>> ipBansSet(List<IpBan> banlist) {
        return client.call("minecraft:ip_bans/set", Map.of("banlist", banlist), new TypeReference<List<IpBan>>() {});
    }

    /**
     * Add ip to ban list
     */
    public CompletableFuture<List<IpBan>> ipBansAdd(List<IncomingIpBan> add) {
        return client.call("minecraft:ip_bans/add", Map.of("add", add), new TypeReference<List<IpBan>>() {});
    }

    /**
     * Remove ip from ban list
     */
    public CompletableFuture<List<IpBan>> ipBansRemove(List<String> ip) {
        return client.call("minecraft:ip_bans/remove", Map.of("ip", ip), new TypeReference<List<IpBan>>() {});
    }

    /**
     * Clear all ips in ban list
     */
    public CompletableFuture<List<IpBan>> ipBansClear() {
        return client.call("minecraft:ip_bans/clear", new TypeReference<List<IpBan>>() {});
    }

    /**
     * Get all connected players
     */
    public CompletableFuture<List<Player>> players() {
        return client.call("minecraft:players", new TypeReference<List<Player>>() {});
    }

    /**
     * Kick players
     */
    public CompletableFuture<List<Player>> playersKick(List<KickPlayer> kick) {
        return client.call("minecraft:players/kick", Map.of("kick", kick), new TypeReference<List<Player>>() {});
    }

    /**
     * Get all oped players
     */
    public CompletableFuture<List<Operator>> operators() {
        return client.call("minecraft:operators", new TypeReference<List<Operator>>() {});
    }

    /**
     * Set all oped players
     */
    public CompletableFuture<List<Operator>> operatorsSet(List<Operator> operators) {
        return client.call("minecraft:operators/set", Map.of("operators", operators), new TypeReference<List<Operator>>() {});
    }

    /**
     * Op players
     */
    public CompletableFuture<List<Operator>> operatorsAdd(List<Operator> add) {
        return client.call("minecraft:operators/add", Map.of("add", add), new TypeReference<List<Operator>>() {});
    }

    /**
     * Deop players
     */
    public CompletableFuture<List<Operator>> operatorsRemove(List<Player> remove) {
        return client.call("minecraft:operators/remove", Map.of("remove", remove), new TypeReference<List<Operator>>() {});
    }

    /**
     * Deop all players
     */
    public CompletableFuture<List<Operator>> operatorsClear() {
        return client.call("minecraft:operators/clear", new TypeReference<List<Operator>>() {});
    }

    /**
     * Get server status
     */
    public CompletableFuture<ServerState> serverStatus() {
        return client.call("minecraft:server/status", new TypeReference<ServerState>() {});
    }

    /**
     * Save server state
     */
    public CompletableFuture<Boolean> serverSave(Boolean flush) {
        return client.call("minecraft:server/save", Map.of("flush", flush), new TypeReference<Boolean>() {});
    }

    /**
     * Stop server
     */
    public CompletableFuture<Boolean> serverStop() {
        return client.call("minecraft:server/stop", new TypeReference<Boolean>() {});
    }

    /**
     * Send a system message
     */
    public CompletableFuture<Boolean> serverSystemMessage(SystemMessage message) {
        return client.call("minecraft:server/system_message", Map.of("message", message), new TypeReference<Boolean>() {});
    }

    /**
     * Get whether automatic world saving is enabled on the server
     */
    public CompletableFuture<Boolean> serversettingsAutosave() {
        return client.call("minecraft:serversettings/autosave", new TypeReference<Boolean>() {});
    }

    /**
     * Enable or disable automatic world saving on the server
     */
    public CompletableFuture<Boolean> serversettingsAutosaveSet(Boolean enable) {
        return client.call("minecraft:serversettings/autosave/set", Map.of("enable", enable), new TypeReference<Boolean>() {});
    }

    /**
     * Get the current difficulty level of the server
     */
    public CompletableFuture<Difficulty> serversettingsDifficulty() {
        return client.call("minecraft:serversettings/difficulty", new TypeReference<Difficulty>() {});
    }

    /**
     * Set the difficulty level of the server
     */
    public CompletableFuture<Difficulty> serversettingsDifficultySet(Difficulty difficulty) {
        return client.call("minecraft:serversettings/difficulty/set", Map.of("difficulty", difficulty), new TypeReference<Difficulty>() {});
    }

    /**
     * Get whether allowlist enforcement is enabled (kicks players immediately when removed from allowlist)
     */
    public CompletableFuture<Boolean> serversettingsEnforceAllowlist() {
        return client.call("minecraft:serversettings/enforce_allowlist", new TypeReference<Boolean>() {});
    }

    /**
     * Enable or disable allowlist enforcement (when enabled, players are kicked immediately upon removal from allowlist)
     */
    public CompletableFuture<Boolean> serversettingsEnforceAllowlistSet(Boolean enforce) {
        return client.call("minecraft:serversettings/enforce_allowlist/set", Map.of("enforce", enforce), new TypeReference<Boolean>() {});
    }

    /**
     * Get whether the allowlist is enabled on the server
     */
    public CompletableFuture<Boolean> serversettingsUseAllowlist() {
        return client.call("minecraft:serversettings/use_allowlist", new TypeReference<Boolean>() {});
    }

    /**
     * Enable or disable the allowlist on the server (controls whether only allowlisted players can join)
     */
    public CompletableFuture<Boolean> serversettingsUseAllowlistSet(Boolean use) {
        return client.call("minecraft:serversettings/use_allowlist/set", Map.of("use", use), new TypeReference<Boolean>() {});
    }

    /**
     * Get the maximum number of players allowed to connect to the server
     */
    public CompletableFuture<Long> serversettingsMaxPlayers() {
        return client.call("minecraft:serversettings/max_players", new TypeReference<Long>() {});
    }

    /**
     * Set the maximum number of players allowed to connect to the server
     */
    public CompletableFuture<Long> serversettingsMaxPlayersSet(Long max) {
        return client.call("minecraft:serversettings/max_players/set", Map.of("max", max), new TypeReference<Long>() {});
    }

    /**
     * Get the number of seconds before the game is automatically paused when no players are online
     */
    public CompletableFuture<Long> serversettingsPauseWhenEmptySeconds() {
        return client.call("minecraft:serversettings/pause_when_empty_seconds", new TypeReference<Long>() {});
    }

    /**
     * Set the number of seconds before the game is automatically paused when no players are online
     */
    public CompletableFuture<Long> serversettingsPauseWhenEmptySecondsSet(Long seconds) {
        return client.call("minecraft:serversettings/pause_when_empty_seconds/set", Map.of("seconds", seconds), new TypeReference<Long>() {});
    }

    /**
     * Get the number of seconds before idle players are automatically kicked from the server
     */
    public CompletableFuture<Long> serversettingsPlayerIdleTimeout() {
        return client.call("minecraft:serversettings/player_idle_timeout", new TypeReference<Long>() {});
    }

    /**
     * Set the number of seconds before idle players are automatically kicked from the server
     */
    public CompletableFuture<Long> serversettingsPlayerIdleTimeoutSet(Long seconds) {
        return client.call("minecraft:serversettings/player_idle_timeout/set", Map.of("seconds", seconds), new TypeReference<Long>() {});
    }

    /**
     * Get whether flight is allowed for players in Survival mode
     */
    public CompletableFuture<Boolean> serversettingsAllowFlight() {
        return client.call("minecraft:serversettings/allow_flight", new TypeReference<Boolean>() {});
    }

    /**
     * Allow or disallow flight for players in Survival mode
     */
    public CompletableFuture<Boolean> serversettingsAllowFlightSet(Boolean allow) {
        return client.call("minecraft:serversettings/allow_flight/set", Map.of("allow", allow), new TypeReference<Boolean>() {});
    }

    /**
     * Get the server's message of the day displayed to players
     */
    public CompletableFuture<String> serversettingsMotd() {
        return client.call("minecraft:serversettings/motd", new TypeReference<String>() {});
    }

    /**
     * Set the server's message of the day displayed to players
     */
    public CompletableFuture<String> serversettingsMotdSet(String message) {
        return client.call("minecraft:serversettings/motd/set", Map.of("message", message), new TypeReference<String>() {});
    }

    /**
     * Get the spawn protection radius in blocks (only operators can edit within this area)
     */
    public CompletableFuture<Long> serversettingsSpawnProtectionRadius() {
        return client.call("minecraft:serversettings/spawn_protection_radius", new TypeReference<Long>() {});
    }

    /**
     * Set the spawn protection radius in blocks (only operators can edit within this area)
     */
    public CompletableFuture<Long> serversettingsSpawnProtectionRadiusSet(Long radius) {
        return client.call("minecraft:serversettings/spawn_protection_radius/set", Map.of("radius", radius), new TypeReference<Long>() {});
    }

    /**
     * Get whether players are forced to use the server's default game mode
     */
    public CompletableFuture<Boolean> serversettingsForceGameMode() {
        return client.call("minecraft:serversettings/force_game_mode", new TypeReference<Boolean>() {});
    }

    /**
     * Enable or disable forcing players to use the server's default game mode
     */
    public CompletableFuture<Boolean> serversettingsForceGameModeSet(Boolean force) {
        return client.call("minecraft:serversettings/force_game_mode/set", Map.of("force", force), new TypeReference<Boolean>() {});
    }

    /**
     * Get the server's default game mode
     */
    public CompletableFuture<GameType> serversettingsGameMode() {
        return client.call("minecraft:serversettings/game_mode", new TypeReference<GameType>() {});
    }

    /**
     * Set the server's default game mode
     */
    public CompletableFuture<GameType> serversettingsGameModeSet(GameType mode) {
        return client.call("minecraft:serversettings/game_mode/set", Map.of("mode", mode), new TypeReference<GameType>() {});
    }

    /**
     * Get the server's view distance in chunks
     */
    public CompletableFuture<Long> serversettingsViewDistance() {
        return client.call("minecraft:serversettings/view_distance", new TypeReference<Long>() {});
    }

    /**
     * Set the server's view distance in chunks
     */
    public CompletableFuture<Long> serversettingsViewDistanceSet(Long distance) {
        return client.call("minecraft:serversettings/view_distance/set", Map.of("distance", distance), new TypeReference<Long>() {});
    }

    /**
     * Get the server's simulation distance in chunks
     */
    public CompletableFuture<Long> serversettingsSimulationDistance() {
        return client.call("minecraft:serversettings/simulation_distance", new TypeReference<Long>() {});
    }

    /**
     * Set the server's simulation distance in chunks
     */
    public CompletableFuture<Long> serversettingsSimulationDistanceSet(Long distance) {
        return client.call("minecraft:serversettings/simulation_distance/set", Map.of("distance", distance), new TypeReference<Long>() {});
    }

    /**
     * Get whether the server accepts player transfers from other servers
     */
    public CompletableFuture<Boolean> serversettingsAcceptTransfers() {
        return client.call("minecraft:serversettings/accept_transfers", new TypeReference<Boolean>() {});
    }

    /**
     * Enable or disable accepting player transfers from other servers
     */
    public CompletableFuture<Boolean> serversettingsAcceptTransfersSet(Boolean accept) {
        return client.call("minecraft:serversettings/accept_transfers/set", Map.of("accept", accept), new TypeReference<Boolean>() {});
    }

    /**
     * Get the interval in seconds between server status heartbeats
     */
    public CompletableFuture<Long> serversettingsStatusHeartbeatInterval() {
        return client.call("minecraft:serversettings/status_heartbeat_interval", new TypeReference<Long>() {});
    }

    /**
     * Set the interval in seconds between server status heartbeats
     */
    public CompletableFuture<Long> serversettingsStatusHeartbeatIntervalSet(Long seconds) {
        return client.call("minecraft:serversettings/status_heartbeat_interval/set", Map.of("seconds", seconds), new TypeReference<Long>() {});
    }

    /**
     * Get default operator permission level
     */
    public CompletableFuture<Long> serversettingsOperatorUserPermissionLevel() {
        return client.call("minecraft:serversettings/operator_user_permission_level", new TypeReference<Long>() {});
    }

    /**
     * Set default operator permission level
     */
    public CompletableFuture<Long> serversettingsOperatorUserPermissionLevelSet(Long level) {
        return client.call("minecraft:serversettings/operator_user_permission_level/set", Map.of("level", level), new TypeReference<Long>() {});
    }

    /**
     * Get whether the server hides online player information from status queries
     */
    public CompletableFuture<Boolean> serversettingsHideOnlinePlayers() {
        return client.call("minecraft:serversettings/hide_online_players", new TypeReference<Boolean>() {});
    }

    /**
     * Enable or disable hiding online player information from status queries
     */
    public CompletableFuture<Boolean> serversettingsHideOnlinePlayersSet(Boolean hide) {
        return client.call("minecraft:serversettings/hide_online_players/set", Map.of("hide", hide), new TypeReference<Boolean>() {});
    }

    /**
     * Get whether the server responds to connection status requests
     */
    public CompletableFuture<Boolean> serversettingsStatusReplies() {
        return client.call("minecraft:serversettings/status_replies", new TypeReference<Boolean>() {});
    }

    /**
     * Enable or disable the server responding to connection status requests
     */
    public CompletableFuture<Boolean> serversettingsStatusRepliesSet(Boolean enable) {
        return client.call("minecraft:serversettings/status_replies/set", Map.of("enable", enable), new TypeReference<Boolean>() {});
    }

    /**
     * Get the entity broadcast range as a percentage
     */
    public CompletableFuture<Long> serversettingsEntityBroadcastRange() {
        return client.call("minecraft:serversettings/entity_broadcast_range", new TypeReference<Long>() {});
    }

    /**
     * Set the entity broadcast range as a percentage
     */
    public CompletableFuture<Long> serversettingsEntityBroadcastRangeSet(Long percentage_points) {
        return client.call("minecraft:serversettings/entity_broadcast_range/set", Map.of("percentage_points", percentage_points), new TypeReference<Long>() {});
    }

    /**
     * Get the available game rule keys and their current values
     */
    public CompletableFuture<List<TypedGameRule>> gamerules() {
        return client.call("minecraft:gamerules", new TypeReference<List<TypedGameRule>>() {});
    }

    /**
     * Update game rule value
     */
    public CompletableFuture<TypedGameRule> gamerulesUpdate(UntypedGameRule gamerule) {
        return client.call("minecraft:gamerules/update", Map.of("gamerule", gamerule), new TypeReference<TypedGameRule>() {});
    }
}
