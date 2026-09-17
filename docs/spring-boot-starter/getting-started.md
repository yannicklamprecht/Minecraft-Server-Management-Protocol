# Getting Started

## Prerequisites

- Java 21+
- Spring Boot 4.x (built and tested against `4.1.1`)
- A reachable Minecraft/Paper server with `management-server-enabled=true` in its
  `server.properties` (see the root [README](../../README.md#run-a-local-paper-server) to spin up
  a local one for testing)

## 1. Make the starter (and its `client` dependency) available

Like the [gradle plugin](../gradle-plugin/getting-started.md#1-make-the-plugin-available-to-your-build),
neither `spring-boot-starter` nor the `client` module it depends on are published anywhere. Pick
whichever fits your situation:

### Option A - same Gradle build (recommended if you've cloned/forked this repo)

This is exactly how `console/build.gradle.kts` does it:

```kotlin
dependencies {
    implementation(project(":spring-boot-starter"))
}
```

### Option B - publish to your local Maven repository

For a genuinely separate project, add the `maven-publish` plugin to both `client/build.gradle.kts`
and `spring-boot-starter/build.gradle.kts`, then:

```bash
./gradlew :client:publishToMavenLocal :spring-boot-starter:publishToMavenLocal
```

and depend on the published coordinates (`group` is `com.github.yannicklamprecht.mc.management` for
every module) from the consuming project's `repositories { mavenLocal() }`.

## 2. Add the dependency and enable autoconfiguration

Spring Boot's autoconfiguration mechanism picks up
`MinecraftManagementAutoConfiguration` automatically via the starter's
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` - no
`@Import`/`@ComponentScan` changes needed in your application. It only activates when
`MinecraftManagementClient` is on the classpath (`@ConditionalOnClass`), which the dependency
above guarantees transitively.

## 3. Configure at least one server

```yaml
# application.yml
minecraft:
  management:
    servers:
      survival:
        url: ws://localhost:25585
        secret: ${SURVIVAL_SECRET}
```

`secret` is the only property without a default - startup fails with a clear
`NullPointerException` message naming the missing property if it's absent for a configured server.
`protocol-version` is deliberately omitted here - see
[protocol-version auto-detection](configuration.md#protocol-version-auto-detection).

See [configuration.md](configuration.md) for every property and how multiple servers work (it's
the same map, just with more entries - there's no dedicated "single server" shorthand).

## 4. Use it

Inject `MinecraftManagementServerRegistry` - not a single `MinecraftManagementSession` bean, since
there can be more than one configured server:

```java
import com.github.yannicklamprecht.mc.management.spring.MinecraftManagementServerRegistry;

@Component
class MyService {
    private final MinecraftManagementServerRegistry registry;

    MyService(MinecraftManagementServerRegistry registry) {
        this.registry = registry;
    }

    void broadcast(String message) {
        registry.session("survival").sendSystemMessage(message);
    }
}
```

By the time any of your own beans can be injected with the registry, every configured server with
`auto-connect: true` (the default) has already had its connection attempted - a server that's
offline or misconfigured is logged as an error and left in the registry in a disconnected state
(calls against it fail with `"Not connected"`) rather than failing your application's startup.

See [api-guide.md](api-guide.md) for the full session API and how to drop down to a
protocol-version-specific facade when you need one.

## 5. React to server activity (optional)

Every MSMP notification is republished as a typed Spring `ApplicationEvent`:

```java
import com.github.yannicklamprecht.mc.management.spring.event.PlayerJoinedEvent;

@EventListener
void onPlayerJoined(PlayerJoinedEvent event) {
    log.info("{} joined {}", event.getPayload().name(), event.getServerId());
}
```

See [events.md](events.md) for the full list and the `ActivityRepository` that persists them
independent of whether anything is listening live.

## Full example

This mirrors the `console` app's own configuration against a local Paper server started via
`./gradlew :server:runServer` from the root of this repo:

```yaml
minecraft:
  management:
    servers:
      skyblock:
        url: ${SKYBLOCK_MANAGEMENT_URL:ws://localhost:25585}
        secret: ${SKYBLOCK_MANAGEMENT_SECRET:LocalDevelopmentManagementSecret12345678}
```
