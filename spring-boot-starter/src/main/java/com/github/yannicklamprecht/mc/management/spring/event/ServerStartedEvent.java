package com.github.yannicklamprecht.mc.management.spring.event;

/**
 * Published for the MSMP minecraft:notification/server/started notification. This notification
 * carries no payload.
 */
public final class ServerStartedEvent extends MinecraftManagementEvent<Void> {

    public ServerStartedEvent(Object source, String serverId) {
        super(source, serverId, null);
    }
}
