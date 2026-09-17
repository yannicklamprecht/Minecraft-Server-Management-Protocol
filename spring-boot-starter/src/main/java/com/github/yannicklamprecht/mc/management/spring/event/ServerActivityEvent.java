package com.github.yannicklamprecht.mc.management.spring.event;

/**
 * Published for the MSMP minecraft:notification/server/activity notification. This notification
 * carries no payload.
 */
public final class ServerActivityEvent extends MinecraftManagementEvent<Void> {

    public ServerActivityEvent(Object source, String serverId) {
        super(source, serverId, null);
    }
}
