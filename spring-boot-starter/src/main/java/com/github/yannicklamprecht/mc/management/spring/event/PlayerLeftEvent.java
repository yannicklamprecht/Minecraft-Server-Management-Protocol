package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/players/left notification.
 */
public final class PlayerLeftEvent extends MinecraftManagementEvent<MinecraftManagementSession.PlayerView> {

    public PlayerLeftEvent(Object source, String serverId, MinecraftManagementSession.PlayerView payload) {
        super(source, serverId, payload);
    }
}
