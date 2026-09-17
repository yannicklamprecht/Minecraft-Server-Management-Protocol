package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/bans/removed notification.
 */
public final class UserBanRemovedEvent extends MinecraftManagementEvent<MinecraftManagementSession.PlayerView> {

    public UserBanRemovedEvent(Object source, String serverId, MinecraftManagementSession.PlayerView payload) {
        super(source, serverId, payload);
    }
}
