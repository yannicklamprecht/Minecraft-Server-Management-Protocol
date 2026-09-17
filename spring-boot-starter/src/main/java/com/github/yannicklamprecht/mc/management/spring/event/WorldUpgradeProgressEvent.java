package com.github.yannicklamprecht.mc.management.spring.event;

/**
 * Published for the MSMP minecraft:notification/world/upgrade_progress notification.
 */
public final class WorldUpgradeProgressEvent extends MinecraftManagementEvent<java.math.BigDecimal> {

    public WorldUpgradeProgressEvent(Object source, String serverId, java.math.BigDecimal payload) {
        super(source, serverId, payload);
    }
}
