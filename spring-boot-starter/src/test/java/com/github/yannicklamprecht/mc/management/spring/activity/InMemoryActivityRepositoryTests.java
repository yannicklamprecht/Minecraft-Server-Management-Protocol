package com.github.yannicklamprecht.mc.management.spring.activity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryActivityRepositoryTests {

    @Test
    void findRecentReturnsEntriesOldestFirst() {
        InMemoryActivityRepository repository = new InMemoryActivityRepository();

        repository.record(entry("survival", "PlayerJoined"));
        repository.record(entry("survival", "PlayerLeft"));

        assertThat(repository.findRecent(10))
                .extracting(ActivityRecord::type)
                .containsExactly("PlayerJoined", "PlayerLeft");
    }

    @Test
    void findRecentHonorsTheRequestedLimit() {
        InMemoryActivityRepository repository = new InMemoryActivityRepository();
        for (int i = 0; i < 5; i++) {
            repository.record(entry("survival", "event-" + i));
        }

        assertThat(repository.findRecent(2))
                .extracting(ActivityRecord::type)
                .containsExactly("event-3", "event-4");
    }

    @Test
    void oldestEntriesAreDroppedOnceCapacityIsExceeded() {
        InMemoryActivityRepository repository = new InMemoryActivityRepository(2);

        repository.record(entry("survival", "first"));
        repository.record(entry("survival", "second"));
        repository.record(entry("survival", "third"));

        assertThat(repository.findRecent(10))
                .extracting(ActivityRecord::type)
                .containsExactly("second", "third");
    }

    @Test
    void capacityMustBePositive() {
        assertThatThrownBy(() -> new InMemoryActivityRepository(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findRecentForServerOnlyReturnsThatServersEntries() {
        InMemoryActivityRepository repository = new InMemoryActivityRepository();

        repository.record(entry("survival", "PlayerJoined"));
        repository.record(entry("creative", "PlayerJoined"));
        repository.record(entry("survival", "PlayerLeft"));

        assertThat(repository.findRecentForServer("survival", 10))
                .extracting(ActivityRecord::type)
                .containsExactly("PlayerJoined", "PlayerLeft");
    }

    private static ActivityRecord entry(String serverId, String type) {
        return new ActivityRecord(serverId, type, null, Instant.now());
    }
}
