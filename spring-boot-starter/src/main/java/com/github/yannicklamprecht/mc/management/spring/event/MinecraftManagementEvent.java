package com.github.yannicklamprecht.mc.management.spring.event;

import org.springframework.context.ApplicationEvent;

/**
 * Base class for every Spring {@link ApplicationEvent} raised in reaction to an MSMP
 * notification. Listeners can either listen for a concrete subclass (e.g.
 * {@link PlayerJoinedEvent}) or for this common base type to receive all notifications.
 * <p>
 * Since a single application can be configured to connect to several MSMP servers at once (see
 * {@code minecraft.management.servers.*}), every event carries the id of the server it came from
 * so listeners can tell them apart.
 *
 * @param <T> the notification payload type, or {@link Void} for payload-less notifications
 */
public abstract class MinecraftManagementEvent<T> extends ApplicationEvent {

    private final String serverId;
    private final T payload;

    protected MinecraftManagementEvent(Object source, String serverId, T payload) {
        super(source);
        this.serverId = serverId;
        this.payload = payload;
    }

    /** The id of the MSMP server (as configured under {@code minecraft.management.servers}) this notification came from. */
    public String getServerId() {
        return serverId;
    }

    public T getPayload() {
        return payload;
    }
}
