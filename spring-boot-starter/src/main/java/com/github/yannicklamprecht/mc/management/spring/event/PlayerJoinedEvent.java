package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/players/joined notification.
 */
public final class PlayerJoinedEvent extends MinecraftManagementEvent<MinecraftManagementSession.PlayerView> {

    public PlayerJoinedEvent(Object source, String serverId, MinecraftManagementSession.PlayerView payload) {
        super(source, serverId, payload);
    }
}
