package com.github.yannicklamprecht.mc.management.spring.event;

/**
 * Published for the MSMP minecraft:notification/server/stopping notification. This notification
 * carries no payload.
 */
public final class ServerStoppingEvent extends MinecraftManagementEvent<Void> {

    public ServerStoppingEvent(Object source, String serverId) {
        super(source, serverId, null);
    }
}
