package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/allowlist/added notification.
 */
public final class AllowlistAddedEvent extends MinecraftManagementEvent<MinecraftManagementSession.PlayerView> {

    public AllowlistAddedEvent(Object source, String serverId, MinecraftManagementSession.PlayerView payload) {
        super(source, serverId, payload);
    }
}
