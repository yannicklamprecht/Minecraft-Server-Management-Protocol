package com.github.yannicklamprecht.mc.management.spring.event;

/**
 * Published for the MSMP minecraft:notification/world/upgrade_started notification. This notification
 * carries no payload.
 */
public final class WorldUpgradeStartedEvent extends MinecraftManagementEvent<Void> {

    public WorldUpgradeStartedEvent(Object source, String serverId) {
        super(source, serverId, null);
    }
}
