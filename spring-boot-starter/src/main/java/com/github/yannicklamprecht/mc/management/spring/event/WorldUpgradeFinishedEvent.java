package com.github.yannicklamprecht.mc.management.spring.event;

/**
 * Published for the MSMP minecraft:notification/world/upgrade_finished notification. This notification
 * carries no payload.
 */
public final class WorldUpgradeFinishedEvent extends MinecraftManagementEvent<Void> {

    public WorldUpgradeFinishedEvent(Object source, String serverId) {
        super(source, serverId, null);
    }
}
