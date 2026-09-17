package com.github.yannicklamprecht.mc.management.spring.event;

/**
 * Published for the MSMP minecraft:notification/server/saved notification. This notification
 * carries no payload.
 */
public final class ServerSavedEvent extends MinecraftManagementEvent<Void> {

    public ServerSavedEvent(Object source, String serverId) {
        super(source, serverId, null);
    }
}
