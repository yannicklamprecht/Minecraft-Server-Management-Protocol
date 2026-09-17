package com.github.yannicklamprecht.mc.management.console.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration attributes for the console's own dashboard UI, as opposed to
 * {@code minecraft.management.*} (see {@code MinecraftManagementProperties}), which configures the
 * MSMP server connections themselves.
 */
@ConfigurationProperties(prefix = "console.ui")
public class ConsoleUiProperties {

    /**
     * When {@code true}, the server list in the dashboard's sidebar only shows servers that are
     * currently connected, instead of every configured server regardless of connection state.
     */
    private boolean hideOfflineServers = false;

    /**
     * Whether kicking/banning a player or making/unmaking one an operator also privately messages
     * every other online operator about it, and whether the dashboard shows a "Notify operators"
     * checkbox letting an admin override that per action. See {@link OperatorNotificationMode}.
     */
    private OperatorNotificationMode operatorNotifications = OperatorNotificationMode.DEFAULT_TRUE;

    public boolean isHideOfflineServers() {
        return hideOfflineServers;
    }

    public void setHideOfflineServers(boolean hideOfflineServers) {
        this.hideOfflineServers = hideOfflineServers;
    }

    public OperatorNotificationMode getOperatorNotifications() {
        return operatorNotifications;
    }

    public void setOperatorNotifications(OperatorNotificationMode operatorNotifications) {
        this.operatorNotifications = operatorNotifications;
    }
}
