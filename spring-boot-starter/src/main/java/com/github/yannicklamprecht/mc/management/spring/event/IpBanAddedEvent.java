package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/ip_bans/added notification.
 */
public final class IpBanAddedEvent extends MinecraftManagementEvent<MinecraftManagementSession.IpBanView> {

    public IpBanAddedEvent(Object source, String serverId, MinecraftManagementSession.IpBanView payload) {
        super(source, serverId, payload);
    }
}
