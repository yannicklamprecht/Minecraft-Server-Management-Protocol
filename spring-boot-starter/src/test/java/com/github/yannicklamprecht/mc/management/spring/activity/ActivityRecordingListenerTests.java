package com.github.yannicklamprecht.mc.management.spring.activity;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.spring.event.PlayerJoinedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ActivityRecordingListenerTests {

    @Test
    void persistsTheEventUnderItsServerIdAndShortTypeName() {
        ActivityRepository repository = mock(ActivityRepository.class);
        ActivityRecordingListener listener = new ActivityRecordingListener(repository);
        MinecraftManagementSession.PlayerView payload = mock(MinecraftManagementSession.PlayerView.class);

        listener.onEvent(new PlayerJoinedEvent(this, "survival", payload));

        ArgumentCaptor<ActivityRecord> captor = ArgumentCaptor.forClass(ActivityRecord.class);
        verify(repository).record(captor.capture());
        ActivityRecord recorded = captor.getValue();
        assertThat(recorded.serverId()).isEqualTo("survival");
        assertThat(recorded.type()).isEqualTo("PlayerJoined");
        assertThat(recorded.payload()).isSameAs(payload);
        assertThat(recorded.timestamp()).isNotNull();
    }
}
