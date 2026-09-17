package com.github.yannicklamprecht.mc.management.spring.event;

/**
 * Published for the MSMP minecraft:notification/ip_bans/removed notification.
 */
public final class IpBanRemovedEvent extends MinecraftManagementEvent<String> {

    public IpBanRemovedEvent(Object source, String serverId, String payload) {
        super(source, serverId, payload);
    }
}
