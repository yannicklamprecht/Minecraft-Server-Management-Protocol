package com.github.yannicklamprecht.mc.management.spring.event;

/**
 * Published for the MSMP minecraft:notification/world/upgrade_failed notification.
 */
public final class WorldUpgradeFailedEvent extends MinecraftManagementEvent<String> {

    public WorldUpgradeFailedEvent(Object source, String serverId, String payload) {
        super(source, serverId, payload);
    }
}
