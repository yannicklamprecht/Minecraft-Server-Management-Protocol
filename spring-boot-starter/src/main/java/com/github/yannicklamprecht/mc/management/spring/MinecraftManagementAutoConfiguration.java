package com.github.yannicklamprecht.mc.management.spring;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRecordingListener;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRepository;
import com.github.yannicklamprecht.mc.management.spring.activity.InMemoryActivityRepository;
import com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Auto-configures connections to one or more Minecraft Server Management Protocol (MSMP) servers,
 * as configured under {@code minecraft.management.servers.*} (see {@link MinecraftManagementProperties}).
 * <p>
 * Wires up, for every configured server:
 * <ul>
 *     <li>a {@link MinecraftManagementClient} and version-agnostic {@link MinecraftManagementSession}</li>
 *     <li>a {@link MinecraftManagementEventBridge} that republishes every MSMP notification as a Spring
 *     {@link org.springframework.context.ApplicationEvent}, stamped with that server's id</li>
 * </ul>
 * all reachable through a single {@link MinecraftManagementServerRegistry} bean, plus one
 * {@link MinecraftManagementClientLifecycle} that connects each server on startup (unless its own
 * {@code auto-connect} is set to {@code false}) and disconnects all of them on shutdown.
 * <p>
 * If no servers are configured, the registry is simply empty and the lifecycle bean does nothing.
 * <p>
 * Also registers an {@link ActivityRepository} (a default, in-memory one unless the application
 * supplies its own) and an {@link ActivityRecordingListener} that persists every published event
 * into it.
 */
@AutoConfiguration
@EnableConfigurationProperties(MinecraftManagementProperties.class)
@ConditionalOnClass(MinecraftManagementClient.class)
public class MinecraftManagementAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MinecraftManagementServerRegistry minecraftManagementServerRegistry(MinecraftManagementProperties properties,
                                                                                ApplicationEventPublisher publisher) {
        Map<String, MinecraftManagementServerRegistry.ManagedServer> servers = new LinkedHashMap<>();
        properties.getServers().forEach((id, serverProperties) -> {
            String secret = Objects.requireNonNull(serverProperties.getSecret(),
                    "minecraft.management.servers." + id + ".secret must be configured");
            MinecraftManagementClient client = new MinecraftManagementClient(new ObjectMapper(), serverProperties.getUrl(), secret);
            // With no explicit protocol-version, session() starts on the latest known protocol as a
            // placeholder; MinecraftManagementClientLifecycle corrects it via auto-detection post-connect.
            MinecraftManagementSession session = serverProperties.isProtocolVersionAutoDetected()
                    ? client.session()
                    : client.session(serverProperties.getProtocolVersion());
            MinecraftManagementEventBridge eventBridge = new MinecraftManagementEventBridge(id, session, publisher);
            servers.put(id, new MinecraftManagementServerRegistry.ManagedServer(id, client, session, eventBridge, serverProperties));
        });
        return new MinecraftManagementServerRegistry(servers);
    }

    @Bean
    @ConditionalOnMissingBean
    public MinecraftManagementClientLifecycle minecraftManagementClientLifecycle(MinecraftManagementServerRegistry registry) {
        return new MinecraftManagementClientLifecycle(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public ActivityRepository activityRepository() {
        return new InMemoryActivityRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public ActivityRecordingListener activityRecordingListener(ActivityRepository activityRepository) {
        return new ActivityRecordingListener(activityRepository);
    }
}
