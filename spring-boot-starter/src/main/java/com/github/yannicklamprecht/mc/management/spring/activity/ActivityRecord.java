package com.github.yannicklamprecht.mc.management.spring.activity;

import com.github.yannicklamprecht.mc.management.spring.event.MinecraftManagementEvent;

import java.time.Instant;

/**
 * One persisted occurrence of a {@link MinecraftManagementEvent}, as recorded by an
 * {@link ActivityRepository}.
 *
 * @param serverId  the id of the MSMP server the event came from (see
 *                  {@link MinecraftManagementEvent#getServerId()})
 * @param type      the notification's short name, e.g. {@code "PlayerJoined"} (the event class's
 *                  simple name with the {@code Event} suffix stripped)
 * @param payload   the event's payload, or {@code null} for payload-less notifications
 * @param timestamp when the event was recorded
 */
public record ActivityRecord(String serverId, String type, Object payload, Instant timestamp) {
}
