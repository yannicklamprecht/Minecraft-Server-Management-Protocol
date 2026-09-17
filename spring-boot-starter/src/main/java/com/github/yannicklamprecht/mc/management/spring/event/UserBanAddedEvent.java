package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/bans/added notification.
 */
public final class UserBanAddedEvent extends MinecraftManagementEvent<MinecraftManagementSession.UserBanView> {

    public UserBanAddedEvent(Object source, String serverId, MinecraftManagementSession.UserBanView payload) {
        super(source, serverId, payload);
    }
}
