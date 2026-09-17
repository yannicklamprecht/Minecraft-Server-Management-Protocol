# Minecraft Server Management Protocol (MSMP) Tooling

This project is a multi-module Java 21 / Gradle workspace built around Minecraft's **Server
Management Protocol (MSMP)** — the JSON-RPC-over-WebSocket API a Minecraft/Paper server can expose
for remote administration (players, operators, allowlist, bans, server settings, game rules, and
live notifications for all of it).

Rather than hand-writing a client against that protocol, this project **generates one**: a Gradle
plugin downloads a Minecraft server for each version you care about, extracts its MSMP OpenRPC
schema, and generates typed Java records, enums, and API facades from it. A Spring Boot starter
then wraps that generated client so a Spring application can connect to one or more MSMP servers
declaratively, with every notification republished as a Spring `ApplicationEvent`.

## Modules

| Module | What it is | Docs |
|---|---|---|
| `gradle-plugin/` | The Gradle plugin that extracts protocol schemas and generates the typed client code. | [Integration guide](gradle-plugin/README.md) |
| `client/` | The generated typed client + the hand-written transport layer (WebSocket, JSON-RPC framing, version-agnostic session facade) it's generated against. Consumed as a library, not usually depended on directly by application code — see the Spring Boot starter instead. | [Generated code reference](gradle-plugin/generated-code.md) |
| `spring-boot-starter/` | Spring Boot autoconfiguration: wires up one client/session per configured server, republishes every MSMP notification as a Spring event, and keeps a queryable activity history. | [Integration guide](spring-boot-starter/README.md) |
| `console/` | A reference application built on the starter: a server-rendered dashboard (players, operators, allowlist, bans, live activity feed) for one or more MSMP servers. Not a library - read its source for a complete worked example of the starter in use. | See the root [README](../README.md#run-the-management-console) |
| `server/` | Local Paper servers (one per generated protocol version) for manual testing, via [jpenilla's run-paper plugin](https://github.com/jpenilla/run-task). | See the root [README](../README.md#run-a-local-paper-server) |

## Where to start

- **Building your own typed MSMP client** (e.g. for a version of the protocol this project
  doesn't already generate, or a different target package): start with the
  [gradle plugin integration guide](gradle-plugin/README.md).
- **Adding MSMP connectivity to a Spring Boot application**: start with the
  [Spring Boot starter integration guide](spring-boot-starter/README.md). This is what the
  `console` app itself is built on.
- **Running the project locally** (a test Paper server + the dashboard) to see it work end to end:
  see the root [README.md](../README.md).

## How the pieces fit together

```
 Minecraft/Paper server jar (per version)
          │  extractMinecraftManagementSchemas
          ▼
   OpenRPC schema (JSON)
          │  generateMinecraftManagementSources     [gradle-plugin]
          ▼
 Typed Java client code (records, enums, API facades, per protocol version)
          │  compiled into                          [client]
          ▼
 MinecraftManagementClient / MinecraftManagementSession
          │  wrapped by                              [spring-boot-starter]
          ▼
 MinecraftManagementServerRegistry (Spring bean) + ApplicationEvents
          │  used by                                 [console, or your app]
          ▼
 Your application
```

A project only needs the gradle plugin if it's *generating* client code (i.e. it owns the
`client`-style module, or an equivalent). Most application code should only ever need the
Spring Boot starter.
