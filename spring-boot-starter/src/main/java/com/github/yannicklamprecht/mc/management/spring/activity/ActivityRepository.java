package com.github.yannicklamprecht.mc.management.spring.activity;

import java.util.List;

/**
 * Persists the stream of {@link com.github.yannicklamprecht.mc.management.spring.event.MinecraftManagementEvent}s so it
 * survives beyond whichever browser tabs happen to be connected when an event is published (see
 * {@code DashboardEventBroadcaster} in the console module, which only forwards events live).
 * <p>
 * The auto-configuration registers a default {@link InMemoryActivityRepository} via
 * {@code @ConditionalOnMissingBean}; an application can override it with its own bean of this
 * type (e.g. one backed by a database) to change how, or how long, activity is retained.
 */
public interface ActivityRepository {

    /** Persists one occurrence of an event. */
    void record(ActivityRecord entry);

    /**
     * The most recently recorded entries, oldest first, newest last.
     *
     * @param limit the maximum number of entries to return
     */
    List<ActivityRecord> findRecent(int limit);

    /**
     * The most recently recorded entries for one server, oldest first, newest last.
     *
     * @param serverId the id of the MSMP server to filter by (see {@link ActivityRecord#serverId()})
     * @param limit    the maximum number of entries to return
     */
    List<ActivityRecord> findRecentForServer(String serverId, int limit);
}
