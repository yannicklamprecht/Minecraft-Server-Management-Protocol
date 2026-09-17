package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/operators/removed notification.
 */
public final class OperatorRemovedEvent extends MinecraftManagementEvent<MinecraftManagementSession.OperatorView> {

    public OperatorRemovedEvent(Object source, String serverId, MinecraftManagementSession.OperatorView payload) {
        super(source, serverId, payload);
    }
}
