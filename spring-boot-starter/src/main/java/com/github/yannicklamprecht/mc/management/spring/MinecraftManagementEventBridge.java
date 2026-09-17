package com.github.yannicklamprecht.mc.management.spring;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.spring.event.AllowlistAddedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.AllowlistRemovedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.GameRuleUpdatedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.IpBanAddedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.IpBanRemovedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.OperatorAddedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.OperatorRemovedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.PlayerJoinedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.PlayerLeftEvent;
import com.github.yannicklamprecht.mc.management.spring.event.ServerActivityEvent;
import com.github.yannicklamprecht.mc.management.spring.event.ServerSavedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.ServerSavingEvent;
import com.github.yannicklamprecht.mc.management.spring.event.ServerStartedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.ServerStatusEvent;
import com.github.yannicklamprecht.mc.management.spring.event.ServerStoppingEvent;
import com.github.yannicklamprecht.mc.management.spring.event.UserBanAddedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.UserBanRemovedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.WorldUpgradeFailedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.WorldUpgradeFinishedEvent;
import com.github.yannicklamprecht.mc.management.spring.event.WorldUpgradeProgressEvent;
import com.github.yannicklamprecht.mc.management.spring.event.WorldUpgradeStartedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Objects;

/**
 * Registers a listener on the {@link MinecraftManagementSession} for every MSMP
 * notification and republishes each one as a Spring {@link org.springframework.context.ApplicationEvent},
 * so the rest of the application can react to server activity with plain {@code @EventListener} methods
 * instead of depending on the MSMP client directly.
 * <p>
 * One bridge exists per configured server (see {@code minecraft.management.servers.*}); every
 * event it publishes is stamped with that server's id so listeners in a multi-server setup can
 * tell notifications apart.
 */
public class MinecraftManagementEventBridge {

    private final String serverId;
    private final MinecraftManagementSession session;
    private final ApplicationEventPublisher publisher;

    public MinecraftManagementEventBridge(String serverId, MinecraftManagementSession session, ApplicationEventPublisher publisher) {
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.session = Objects.requireNonNull(session, "session");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    /**
     * Wires up one listener per notification. Safe to call once, before the session connects.
     */
    public void registerListeners() {
        session.onPlayerJoined(player -> publisher.publishEvent(new PlayerJoinedEvent(this, serverId, player)));
        session.onPlayerLeft(player -> publisher.publishEvent(new PlayerLeftEvent(this, serverId, player)));
        session.onServerStatus(status -> publisher.publishEvent(new ServerStatusEvent(this, serverId, status)));
        session.onServerStarted(() -> publisher.publishEvent(new ServerStartedEvent(this, serverId)));
        session.onServerStopping(() -> publisher.publishEvent(new ServerStoppingEvent(this, serverId)));
        session.onServerSaved(() -> publisher.publishEvent(new ServerSavedEvent(this, serverId)));
        session.onServerSaving(() -> publisher.publishEvent(new ServerSavingEvent(this, serverId)));

        // Server activity notifications require protocol 2.0.0 or later.
        if (!"1.0.0".equals(session.protocolVersion())) {
            session.onServerActivity(() -> publisher.publishEvent(new ServerActivityEvent(this, serverId)));
        }

        session.onOperatorAdded(operator -> publisher.publishEvent(new OperatorAddedEvent(this, serverId, operator)));
        session.onOperatorRemoved(operator -> publisher.publishEvent(new OperatorRemovedEvent(this, serverId, operator)));
        session.onAllowlistAdded(player -> publisher.publishEvent(new AllowlistAddedEvent(this, serverId, player)));
        session.onAllowlistRemoved(player -> publisher.publishEvent(new AllowlistRemovedEvent(this, serverId, player)));
        session.onIpBanAdded(ban -> publisher.publishEvent(new IpBanAddedEvent(this, serverId, ban)));
        session.onIpBanRemoved(ip -> publisher.publishEvent(new IpBanRemovedEvent(this, serverId, ip)));
        session.onUserBanAdded(ban -> publisher.publishEvent(new UserBanAddedEvent(this, serverId, ban)));
        session.onUserBanRemoved(player -> publisher.publishEvent(new UserBanRemovedEvent(this, serverId, player)));
        session.onGameRuleUpdated(gameRule -> publisher.publishEvent(new GameRuleUpdatedEvent(this, serverId, gameRule)));

        // World upgrade notifications were only added in protocol 3.1.0.
        if ("3.1.0".equals(session.protocolVersion())) {
            session.onWorldUpgradeStarted(() -> publisher.publishEvent(new WorldUpgradeStartedEvent(this, serverId)));
            session.onWorldUpgradeProgress(progress -> publisher.publishEvent(new WorldUpgradeProgressEvent(this, serverId, progress)));
            session.onWorldUpgradeFinished(() -> publisher.publishEvent(new WorldUpgradeFinishedEvent(this, serverId)));
            session.onWorldUpgradeFailed(reason -> publisher.publishEvent(new WorldUpgradeFailedEvent(this, serverId, reason)));
        }
    }
}
