package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/allowlist/removed notification.
 */
public final class AllowlistRemovedEvent extends MinecraftManagementEvent<MinecraftManagementSession.PlayerView> {

    public AllowlistRemovedEvent(Object source, String serverId, MinecraftManagementSession.PlayerView payload) {
        super(source, serverId, payload);
    }
}
