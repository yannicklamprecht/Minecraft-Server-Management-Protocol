package com.github.yannicklamprecht.mc.management.spring.event;

/**
 * Published for the MSMP minecraft:notification/server/saving notification. This notification
 * carries no payload.
 */
public final class ServerSavingEvent extends MinecraftManagementEvent<Void> {

    public ServerSavingEvent(Object source, String serverId) {
        super(source, serverId, null);
    }
}
