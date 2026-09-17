package com.github.yannicklamprecht.mc.management.spring;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRecord;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRecordingListener;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRepository;
import com.github.yannicklamprecht.mc.management.spring.activity.InMemoryActivityRepository;
import com.github.yannicklamprecht.mc.management.spring.event.PlayerLeftEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MinecraftManagementAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MinecraftManagementAutoConfiguration.class));

    @Test
    void registryIsEmptyWithoutAnyServerConfigured() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MinecraftManagementServerRegistry.class);
            assertThat(context.getBean(MinecraftManagementServerRegistry.class).isEmpty()).isTrue();
        });
    }

    @Test
    void singleNamedServerIsWiredUp() {
        contextRunner
                .withPropertyValues(
                        "minecraft.management.servers.survival.secret=test-secret",
                        // Do not attempt a real network connection while running the test suite.
                        "minecraft.management.servers.survival.auto-connect=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(MinecraftManagementServerRegistry.class);
                    assertThat(context).hasSingleBean(MinecraftManagementClientLifecycle.class);

                    MinecraftManagementServerRegistry registry = context.getBean(MinecraftManagementServerRegistry.class);
                    assertThat(registry.serverIds()).containsExactly("survival");
                    assertThat(registry.session("survival").protocolVersion()).isEqualTo("3.1.0");
                });
    }

    @Test
    void multipleNamedServersAreEachWiredUpIndependently() {
        contextRunner
                .withPropertyValues(
                        "minecraft.management.servers.survival.secret=survival-secret",
                        "minecraft.management.servers.survival.url=ws://localhost:25585",
                        "minecraft.management.servers.survival.auto-connect=false",
                        "minecraft.management.servers.creative.secret=creative-secret",
                        "minecraft.management.servers.creative.url=ws://localhost:25586",
                        "minecraft.management.servers.creative.protocol-version=1.0.0",
                        "minecraft.management.servers.creative.auto-connect=false")
                .run(context -> {
                    MinecraftManagementServerRegistry registry = context.getBean(MinecraftManagementServerRegistry.class);
                    assertThat(registry.serverIds()).containsExactlyInAnyOrder("survival", "creative");

                    assertThat(registry.session("survival").protocolVersion()).isEqualTo("3.1.0");
                    assertThat(registry.session("creative").protocolVersion()).isEqualTo("1.0.0");

                    // Each server gets its own client/session instance.
                    assertThat(registry.get("survival").client()).isNotSameAs(registry.get("creative").client());
                });
    }

    @Test
    void defaultActivityRepositoryIsInMemoryAndFedByPublishedEvents() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ActivityRepository.class);
            assertThat(context.getBean(ActivityRepository.class)).isInstanceOf(InMemoryActivityRepository.class);
            assertThat(context).hasSingleBean(ActivityRecordingListener.class);

            context.publishEvent(new PlayerLeftEvent(this, "survival", mock(MinecraftManagementSession.PlayerView.class)));

            List<ActivityRecord> recent = context.getBean(ActivityRepository.class).findRecent(10);
            assertThat(recent).hasSize(1);
            assertThat(recent.get(0).serverId()).isEqualTo("survival");
            assertThat(recent.get(0).type()).isEqualTo("PlayerLeft");
        });
    }

    @Test
    void anApplicationCanOverrideTheDefaultActivityRepository() {
        contextRunner
                .withUserConfiguration(CustomActivityRepositoryConfiguration.class)
                .run(context -> assertThat(context.getBean(ActivityRepository.class)).isNotInstanceOf(InMemoryActivityRepository.class));
    }

    @Configuration
    static class CustomActivityRepositoryConfiguration {
        @Bean
        ActivityRepository activityRepository() {
            return new ActivityRepository() {
                @Override
                public void record(ActivityRecord entry) {
                    // not exercised by this test
                }

                @Override
                public List<ActivityRecord> findRecent(int limit) {
                    return List.of();
                }

                @Override
                public List<ActivityRecord> findRecentForServer(String serverId, int limit) {
                    return List.of();
                }
            };
        }
    }
}
