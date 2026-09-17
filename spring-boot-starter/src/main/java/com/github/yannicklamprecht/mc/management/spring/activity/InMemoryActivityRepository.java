package com.github.yannicklamprecht.mc.management.spring.activity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Default {@link ActivityRepository}: keeps the most recent entries in memory, discarding the
 * oldest once {@code capacity} is exceeded. Cleared on restart - applications that need activity
 * to survive a restart should provide their own {@link ActivityRepository} bean instead.
 */
public class InMemoryActivityRepository implements ActivityRepository {

    /** Used when no explicit capacity is given; comfortably covers a single dashboard session. */
    public static final int DEFAULT_CAPACITY = 500;

    private final int capacity;
    private final Deque<ActivityRecord> entries = new ArrayDeque<>();

    public InMemoryActivityRepository() {
        this(DEFAULT_CAPACITY);
    }

    public InMemoryActivityRepository(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, was " + capacity);
        }
        this.capacity = capacity;
    }

    @Override
    public synchronized void record(ActivityRecord entry) {
        entries.addLast(entry);
        while (entries.size() > capacity) {
            entries.removeFirst();
        }
    }

    @Override
    public synchronized List<ActivityRecord> findRecent(int limit) {
        return lastN(List.copyOf(entries), limit);
    }

    @Override
    public synchronized List<ActivityRecord> findRecentForServer(String serverId, int limit) {
        List<ActivityRecord> matching = entries.stream()
                .filter(entry -> entry.serverId().equals(serverId))
                .toList();
        return lastN(matching, limit);
    }

    private static List<ActivityRecord> lastN(List<ActivityRecord> entries, int limit) {
        return entries.subList(Math.max(0, entries.size() - limit), entries.size());
    }
}
