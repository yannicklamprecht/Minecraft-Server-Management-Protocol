package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/server/status notification.
 */
public final class ServerStatusEvent extends MinecraftManagementEvent<MinecraftManagementSession.ServerStatusView> {

    public ServerStatusEvent(Object source, String serverId, MinecraftManagementSession.ServerStatusView payload) {
        super(source, serverId, payload);
    }
}
