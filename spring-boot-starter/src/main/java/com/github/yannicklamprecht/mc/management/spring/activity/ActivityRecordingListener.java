package com.github.yannicklamprecht.mc.management.spring.activity;

import com.github.yannicklamprecht.mc.management.spring.event.MinecraftManagementEvent;
import org.springframework.context.event.EventListener;

import java.time.Instant;

/**
 * Bridges every published {@link MinecraftManagementEvent} into the configured
 * {@link ActivityRepository}. Listening for the generic base type is enough to capture every
 * concrete notification the starter publishes, across every configured server.
 */
public class ActivityRecordingListener {

    private final ActivityRepository repository;

    public ActivityRecordingListener(ActivityRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onEvent(MinecraftManagementEvent<?> event) {
        repository.record(new ActivityRecord(event.getServerId(), toType(event), event.getPayload(), Instant.now()));
    }

    /** {@code PlayerJoinedEvent} -&gt; {@code PlayerJoined} */
    private static String toType(MinecraftManagementEvent<?> event) {
        String className = event.getClass().getSimpleName();
        return className.endsWith("Event") ? className.substring(0, className.length() - "Event".length()) : className;
    }
}
