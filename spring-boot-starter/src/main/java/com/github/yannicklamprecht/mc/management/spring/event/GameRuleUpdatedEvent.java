package com.github.yannicklamprecht.mc.management.spring.event;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;

/**
 * Published for the MSMP minecraft:notification/gamerules/updated notification.
 */
public final class GameRuleUpdatedEvent extends MinecraftManagementEvent<MinecraftManagementSession.GameRuleView> {

    public GameRuleUpdatedEvent(Object source, String serverId, MinecraftManagementSession.GameRuleView payload) {
        super(source, serverId, payload);
    }
}
