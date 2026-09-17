package com.github.yannicklamprecht.mc.management.v1_0_0;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient;
import com.github.yannicklamprecht.mc.management.v1_0_0.dto.IpBan;
import com.github.yannicklamprecht.mc.management.v1_0_0.dto.Operator;
import com.github.yannicklamprecht.mc.management.v1_0_0.dto.Player;
import com.github.yannicklamprecht.mc.management.v1_0_0.dto.ServerState;
import com.github.yannicklamprecht.mc.management.v1_0_0.dto.TypedGameRule;
import com.github.yannicklamprecht.mc.management.v1_0_0.dto.UserBan;
import java.lang.Runnable;
import java.lang.String;
import java.util.function.Consumer;

/**
 * Typed notification listeners for Minecraft Management Protocol v1.0.0.
 */
public final class MinecraftManagementNotifications {
    private final MinecraftManagementClient client;

    public MinecraftManagementNotifications(MinecraftManagementClient client) {
        this.client = client;
    }

    public MinecraftManagementClient client() {
        return client;
    }

    /**
     * Server started
     */
    public void onServerStarted(Runnable listener) {
        client.registerNotification("minecraft:notification/server/started", listener);
    }

    /**
     * Server shutting down
     */
    public void onServerStopping(Runnable listener) {
        client.registerNotification("minecraft:notification/server/stopping", listener);
    }

    /**
     * Server save started
     */
    public void onServerSaving(Runnable listener) {
        client.registerNotification("minecraft:notification/server/saving", listener);
    }

    /**
     * Server save completed
     */
    public void onServerSaved(Runnable listener) {
        client.registerNotification("minecraft:notification/server/saved", listener);
    }

    /**
     * Player joined
     */
    public void onPlayersJoined(Consumer<Player> listener) {
        client.registerNotificationProperty("minecraft:notification/players/joined", "player", new TypeReference<Player>() {}, listener);
    }

    /**
     * Player left
     */
    public void onPlayersLeft(Consumer<Player> listener) {
        client.registerNotificationProperty("minecraft:notification/players/left", "player", new TypeReference<Player>() {}, listener);
    }

    /**
     * Player was oped
     */
    public void onOperatorsAdded(Consumer<Operator> listener) {
        client.registerNotificationProperty("minecraft:notification/operators/added", "player", new TypeReference<Operator>() {}, listener);
    }

    /**
     * Player was deoped
     */
    public void onOperatorsRemoved(Consumer<Operator> listener) {
        client.registerNotificationProperty("minecraft:notification/operators/removed", "player", new TypeReference<Operator>() {}, listener);
    }

    /**
     * Player was added to allowlist
     */
    public void onAllowlistAdded(Consumer<Player> listener) {
        client.registerNotificationProperty("minecraft:notification/allowlist/added", "player", new TypeReference<Player>() {}, listener);
    }

    /**
     * Player was removed from allowlist
     */
    public void onAllowlistRemoved(Consumer<Player> listener) {
        client.registerNotificationProperty("minecraft:notification/allowlist/removed", "player", new TypeReference<Player>() {}, listener);
    }

    /**
     * Ip was added to ip ban list
     */
    public void onIpBansAdded(Consumer<IpBan> listener) {
        client.registerNotificationProperty("minecraft:notification/ip_bans/added", "player", new TypeReference<IpBan>() {}, listener);
    }

    /**
     * Ip was removed from ip ban list
     */
    public void onIpBansRemoved(Consumer<String> listener) {
        client.registerNotificationProperty("minecraft:notification/ip_bans/removed", "player", new TypeReference<String>() {}, listener);
    }

    /**
     * Player was added to ban list
     */
    public void onBansAdded(Consumer<UserBan> listener) {
        client.registerNotificationProperty("minecraft:notification/bans/added", "player", new TypeReference<UserBan>() {}, listener);
    }

    /**
     * Player was removed from ban list
     */
    public void onBansRemoved(Consumer<Player> listener) {
        client.registerNotificationProperty("minecraft:notification/bans/removed", "player", new TypeReference<Player>() {}, listener);
    }

    /**
     * Gamerule was changed
     */
    public void onGamerulesUpdated(Consumer<TypedGameRule> listener) {
        client.registerNotificationProperty("minecraft:notification/gamerules/updated", "gamerule", new TypeReference<TypedGameRule>() {}, listener);
    }

    /**
     * Server status heartbeat
     */
    public void onServerStatus(Consumer<ServerState> listener) {
        client.registerNotificationProperty("minecraft:notification/server/status", "status", new TypeReference<ServerState>() {}, listener);
    }
}
