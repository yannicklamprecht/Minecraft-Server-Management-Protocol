package com.github.yannicklamprecht.mc.management.console.web;

import com.github.yannicklamprecht.mc.management.spring.event.MinecraftManagementEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bridges the library's {@link MinecraftManagementEvent} hierarchy to Server-Sent Events, so the
 * plain-JS frontend can subscribe to {@code /api/events} and update the page live instead of polling.
 * <p>
 * Listening for the generic {@link MinecraftManagementEvent} base type is enough to receive every
 * concrete notification event the starter publishes - no per-notification listener method needed.
 * <p>
 * A single stream carries notifications from every configured server; each event's data is wrapped
 * with its {@code serverId} so the frontend (which may be showing any one of several servers) can
 * tell them apart and ignore events for servers the user isn't currently looking at.
 */
@Component
public class DashboardEventBroadcaster {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardEventBroadcaster.class);

    /** Wire shape sent to the browser for every notification. */
    private record SseEventPayload(String serverId, Object payload) {
    }

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        return emitter;
    }

    @EventListener
    public void onNotification(MinecraftManagementEvent<?> event) {
        String eventName = toEventName(event.getClass().getSimpleName());
        SseEventPayload payload = new SseEventPayload(event.getServerId(), event.getPayload());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                // Expected whenever a browser tab closes/navigates away while the stream is open
                // (surfaces as a broken pipe on the next write) - not actionable, so skip the
                // stack trace and just note that the emitter was dropped.
                LOGGER.debug("Dropping SSE emitter after failed send: {}", e.toString());
                emitters.remove(emitter);
            }
        }
    }

    /** {@code PlayerJoinedEvent} -&gt; {@code player-joined} */
    private static String toEventName(String className) {
        String withoutSuffix = className.endsWith("Event") ? className.substring(0, className.length() - "Event".length()) : className;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < withoutSuffix.length(); i++) {
            char c = withoutSuffix.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('-');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }
}
