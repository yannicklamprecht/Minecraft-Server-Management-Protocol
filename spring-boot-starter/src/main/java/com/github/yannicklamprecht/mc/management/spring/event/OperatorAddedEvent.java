package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/operators/added notification.
 */
public final class OperatorAddedEvent extends MinecraftManagementEvent<MinecraftManagementSession.OperatorView> {

    public OperatorAddedEvent(Object source, String serverId, MinecraftManagementSession.OperatorView payload) {
        super(source, serverId, payload);
    }
}
