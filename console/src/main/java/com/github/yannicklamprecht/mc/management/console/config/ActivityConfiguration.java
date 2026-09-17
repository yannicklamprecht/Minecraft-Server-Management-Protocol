package com.github.yannicklamprecht.mc.management.console.config;

import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRepository;
import com.github.yannicklamprecht.mc.management.spring.activity.InMemoryActivityRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Overrides the starter's default {@link ActivityRepository} (registered only via
 * {@code @ConditionalOnMissingBean} in {@code MinecraftManagementAutoConfiguration}) with a
 * larger history: the dashboard's activity feed is meant to cover a whole session at the console,
 * not just the library's default handful of recent events.
 */
@Configuration
public class ActivityConfiguration {

    private static final int CONSOLE_ACTIVITY_HISTORY_SIZE = 2000;

    @Bean
    public ActivityRepository activityRepository() {
        return new InMemoryActivityRepository(CONSOLE_ACTIVITY_HISTORY_SIZE);
    }
}
