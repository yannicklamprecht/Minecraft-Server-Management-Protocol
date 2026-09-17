# Spring Boot Starter Integration Guide

The `spring-boot-starter` module autoconfigures a Spring Boot application to connect to one or
more Minecraft Server Management Protocol (MSMP) servers declaratively, wrapping the generated
[client](../gradle-plugin/generated-code.md) so application code never has to construct a
`MinecraftManagementClient` by hand.

For each server configured under `minecraft.management.servers.<id>.*`, it:

- Creates and connects a `MinecraftManagementClient` / version-agnostic `MinecraftManagementSession`
  pair on application startup (unless that server's `auto-connect` is `false`).
- Auto-detects the server's actual MSMP protocol version if none is configured explicitly.
- Republishes every MSMP notification (player joined/left, operator/allowlist/ban changes, server
  saves, world upgrades, ...) as a Spring `ApplicationEvent`, stamped with the id of the server it
  came from.
- Records that same event stream into a queryable `ActivityRepository` (in-memory by default,
  replaceable with your own bean).
- Tolerates one server being offline or misconfigured without failing application startup or
  affecting the other configured servers.

This is exactly what the `console/` app (a server-rendered dashboard) is built on - its source is
a complete worked example beyond what's shown here.

## Contents

- [Getting started](getting-started.md) - add the dependency, configure a server, use it.
- [Configuration reference](configuration.md) - every `minecraft.management.*` property, multi-server
  setup, and protocol-version auto-detection.
- [API guide](api-guide.md) - `MinecraftManagementServerRegistry`, the session API, and dropping
  down to the raw client when you need to.
- [Events reference](events.md) - every published `ApplicationEvent`, their payloads, and the
  activity repository.

## At a glance

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":spring-boot-starter")) // see getting-started.md for external consumption
}
```

```yaml
# application.yml
minecraft:
  management:
    servers:
      survival:
        url: ws://localhost:25585
        secret: ${SURVIVAL_SECRET}
```

```java
@Component
class MyService {
    MyService(MinecraftManagementServerRegistry registry) {
        registry.session("survival").getPlayers()
                .thenAccept(players -> players.forEach(System.out::println));
    }

    @EventListener
    void onPlayerJoined(PlayerJoinedEvent event) {
        System.out.println(event.getPayload().name() + " joined " + event.getServerId());
    }
}
```
