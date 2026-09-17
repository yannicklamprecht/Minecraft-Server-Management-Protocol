# Minecraft Server Management Protocol (MSMP)

A multi-module Java 21 / Gradle project for extracting Minecraft Server Management Protocol (MSMP) OpenRPC schemas from server JARs and generating typed Java client bindings.

## Project Structure

- **`gradle-plugin/`** — Gradle plugin (`com.github.yannicklamprecht.mc.management.schema-extractor`) that:
  - Fetches Minecraft version manifests from Mojang
  - Downloads server JARs for specified versions
  - Executes data generation to extract protocol schemas
  - Outputs OpenRPC schemas as JSON
  
- **`client/`** — Java 21 client library providing:
  - WebSocket transport layer
  - Automatic code generation from OpenRPC schemas
  - Typed DTOs, records, enums, and API facades

- **`spring-boot-starter/`** — Spring Boot autoconfiguration for the client:
  - `minecraft.management.servers.<id>.*` configuration properties (url, secret, protocol version, auto-connect) for **one or more** named MSMP servers
  - Autoconfigures a `MinecraftManagementServerRegistry` holding one `MinecraftManagementClient`/`MinecraftManagementSession` pair per configured server, and connects each on startup
  - Publishes a Spring `ApplicationEvent` (see `com.github.yannicklamprecht.mc.management.spring.event`) for every MSMP notification, stamped with the id of the server it came from

- **`console/`** — Spring Boot web app built on `spring-boot-starter`:
  - Server-rendered dashboard using [jte](https://jte.gg) templates (`src/main/jte`)
  - A JSON API under `/api/servers` (list configured servers) and `/api/servers/{serverId}/**` (players, allowlist, operators, bans) for each one
  - A `/api/events` Server-Sent-Events stream pushing every MSMP notification, from every configured server, live to the browser (each event carries its `serverId`)
  - A plain-JS frontend (`src/main/resources/static/app.js`, no build step) with a server selector driving the dashboard

## Getting Started

### Run a Local Paper Server

The `server/` module uses [jpenilla's run-paper plugin](https://github.com/jpenilla/run-task)
to download and run Paper without building the client or extracting schemas:

```bash
./gradlew :server:runServer
```

The task accepts the Minecraft EULA automatically. On first launch it copies
`server/server.properties` into the ignored `server/run/` directory, enabling the
management server at `ws://localhost:25585` with TLS disabled for local development.
The local-development authentication secret is `LocalDevelopmentManagementSecret12345678`.
Run the client in another terminal; Gradle supplies the secret automatically:

```bash
./gradlew :client:run
```

Subsequent server runs preserve configuration changes and existing secrets, filling in
the default secret only when missing or blank. The client run task reads the secret
from `server/run/server.properties`, falling back to `server/server.properties` if
the local secret is missing or blank. An explicitly set `MINECRAFT_MANAGEMENT_SECRET`
environment variable takes precedence.
Worlds and other runtime files also
live in `server/run/`. Enter `stop` in the server console to shut it down.

#### Running Multiple Paper Servers in Parallel

`server/build.gradle.kts` registers one `runServer*` task per MSMP protocol/JSON-RPC version
generated under `client/src/generated/java/.../v<protocol>/`, each running a different Minecraft
version so every generated API version can be exercised against a real server:

| Task | Minecraft version | MSMP protocol | Generated package | Game port | Management port |
|------|--------------------|----------------|---------------------|-----------|------------------|
| `:server:runServerV1_0_0` | 1.21.10 | 1.0.0 | `v1_0_0` | 25566 | 25586 |
| `:server:runServer` | 1.21.11 | 2.0.0 | `v2_0_0` | 25565 | 25585 |
| `:server:runServerV3_0_0` | 26.2 | 3.0.0 | `v3_0_0` | 25567 | 25587 |
| `:server:runServerV3_1_0` | 26.3 | 3.1.0 | `v3_1_0` | 25568 | 25588 |

Each task gets its own run directory (`server/run/`, `server/run-v1_0_0/`, `server/run-v3_0_0/`,
`server/run-v3_1_0/`) and its own seeded `server.properties` (`server/server.properties`,
`server/server-v1_0_0.properties`, ...), so any combination can run at the same time without port
or world conflicts - each blocks the console it's started in, so run each in its own terminal:

```bash
./gradlew :server:runServer          # terminal 1 - Minecraft 1.21.11, protocol 2.0.0
./gradlew :server:runServerV1_0_0    # terminal 2 - Minecraft 1.21.10, protocol 1.0.0
./gradlew :server:runServerV3_0_0    # terminal 3 - Minecraft 26.2,   protocol 3.0.0
./gradlew :server:runServerV3_1_0    # terminal 4 - Minecraft 26.3,   protocol 3.1.0
```

Point the console (or any client) at one of them per configured server id, matching each
server's management port from the table above - `protocol-version` can be left unset and is
auto-detected on connect (see below), so it's omitted here:

```yaml
minecraft:
  management:
    servers:
      v1_0_0:
        url: ws://localhost:25586
        secret: LocalDevelopmentManagementSecret12345678
      v3_1_0:
        url: ws://localhost:25588
        secret: LocalDevelopmentManagementSecret12345678
```

### Run the Management Console

With the local Paper server running, start the web console in another terminal:

```bash
./gradlew :console:bootRun
```

Open [http://localhost:8080](http://localhost:8080) for the dashboard. The console connects
to every configured MSMP server on startup and stays connected for as long as the app runs;
every notification from each server (players joining/leaving, allowlist/operator/ban changes,
saves, etc.) is pushed live to the page over Server-Sent Events.

Every server is configured by name under `minecraft.management.servers.<id>.*` (see
`console/src/main/resources/application.yml`) - there is no single-server shorthand, only the map:

| Property | Default | Description |
|----------|---------|--------------|
| `minecraft.management.servers.<id>.url` | `ws://localhost:25585` | MSMP WebSocket URL |
| `minecraft.management.servers.<id>.secret` | — | Bearer secret (required for this server to be configured) |
| `minecraft.management.servers.<id>.protocol-version` | *auto-detected* | MSMP protocol version to speak; leave unset to auto-detect (see below), or pin one explicitly |
| `minecraft.management.servers.<id>.auto-connect` | `true` | Connect automatically on startup; set `false` for manual/test control |

```yaml
minecraft:
  management:
    servers:
      survival:
        url: ws://localhost:25585
        secret: ${SURVIVAL_SECRET}
      creative:
        url: ws://localhost:25586
        secret: ${CREATIVE_SECRET}
```

Every configured server gets its own websocket connection and shows up in the dashboard's server
selector, and in the API under `GET /api/servers` and `/api/servers/{id}/**`.

#### Protocol Version Auto-Detection

`protocol-version` is optional. Left unset (the default), each server's actual MSMP protocol
version is detected right after connecting instead of being assumed: there's no dedicated
"get MSMP version" RPC method, so this works by calling `minecraft:server/status` (supported by
every protocol version) and mapping its `version.name` (the Minecraft version, e.g. `"1.21.11"`)
through a generated `ProtocolVersions.MINECRAFT_VERSION_TO_PROTOCOL_VERSION` table -
`client/src/generated/java/.../ProtocolVersions.java`, produced by the schema-extractor Gradle
plugin alongside the per-protocol-version DTOs/API classes from the same extracted schemas (see
[Extract Protocol Schemas](#2-extract-protocol-schemas) below). Rerunning
`extractMinecraftManagementSchemas`/`generateMinecraftManagementSources` regenerates this table, so
newer Minecraft/protocol versions are picked up without any hand-written mapping to maintain. A
Minecraft version the table doesn't recognize (older than `minMinecraftVersion`, or newer than the
last regeneration) falls back to the latest known protocol version, logged as a warning.

Set `protocol-version` explicitly on a server to skip this detection round-trip or to pin a
specific version regardless of what the server reports.

The checked-in `console/src/main/resources/application.yml` configures one server, id `skyblock`,
reading `SKYBLOCK_MANAGEMENT_URL`/`SKYBLOCK_MANAGEMENT_SECRET` (defaulting to
`ws://localhost:25585`, matching the local Paper server from `:server:runServer` above, when
unset). The `:console:bootRun` task auto-supplies `SKYBLOCK_MANAGEMENT_SECRET` the same way
`:client:run` does for the plain client, reading it from `server/run/server.properties` (falling
back to `server/server.properties`) - so `:console:bootRun` connects out of the box as long as the
local Paper server is already running.

A server that fails to connect (wrong port, offline, bad secret, ...) is logged as an error but
does not stop the console from starting or stop its other configured servers from connecting;
calls against it just fail with "Not connected" until it's fixed and the app is restarted. It
still shows up in the sidebar's server list, badged "offline" in red, unless `console.ui.hide-offline-servers`
is set to `true` (default `false`), in which case it's left out of the list entirely instead.

Each sidebar entry shows the server's Minecraft version (or "offline") rather than its MSMP
protocol version - hover it to see the protocol version instead. The sidebar re-checks every
configured server's connection state and Minecraft version every 10 seconds, independent of the
currently selected server, since that's the only way a server going online/offline is ever
reflected there.

### 1. Configure the Plugin

Apply the plugin and configure package generation in your `build.gradle.kts`:

```kotlin
plugins {
    id("com.github.yannicklamprecht.mc.management.schema-extractor")
}

minecraftManagement {
    packageName.set("com.github.yannicklamprecht.mc.management") // Required: target package for generated code
    onlyReleases.set(true)                             // default: true (only release versions, no snapshots)
    minMinecraftVersion.set("1.21.9")                  // default: 1.21.9 (when MSMP was introduced)
    // Optional: specify custom versions
    // versions.set(listOf("1.21.9", "1.21.10"))
}
```

### 2. Extract Protocol Schemas

Download Minecraft servers and extract OpenRPC schemas:

```bash
./gradlew extractMinecraftManagementSchemas
```

Alias:
```bash
./gradlew extractProtocolSchemas
```

Schemas are cached in `.gradle/caches/minecraft-management-gradle-plugin/protocol-schemas/`.

### 3. Generate Typed Sources

Build the project to automatically generate typed Java code from schemas:

```bash
./gradlew build
```

Generated sources are placed in `src/generated/java/` and automatically included in compilation.

Generated classes include:
- **DTOs and Records** — Strongly typed data structures for protocol messages
- **Enums** — Protocol constants and options
- **API Facades** — Version-specific API clients (e.g., `com.github.yannicklamprecht.mc.management.generated.v1_0_0.MinecraftManagementApi`)

## Configuration Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `packageName` | String | — | **Required.** Package name for generated sources (e.g., `com.github.yannicklamprecht.mc.management.generated`) |
| `onlyReleases` | Boolean | `true` | Include only stable releases (exclude snapshots/RCs) |
| `minMinecraftVersion` | String | `1.21.9` | Earliest version to extract schemas for |
| `versions` | List<String> | empty | If specified, extract only these versions (overrides `minMinecraftVersion`) |
| `manifestUrl` | String | Mojang official | URL of Minecraft version manifest |
| `outputDir` | Directory | `.gradle/caches/minecraft-management-gradle-plugin/protocol-schemas` | Schema output directory |
| `cacheDir` | Directory | `.gradle/caches/minecraft-management-gradle-plugin/minecraft-servers` | Downloaded server JARs cache |
| `generatedSourcesDir` | Directory | `src/generated/java` | Where generated Java code is written |

## Available Tasks

| Task | Aliases | Description |
|------|---------|-------------|
| `extractMinecraftManagementSchemas` | `extractProtocolSchemas` | Download servers and extract schemas |
| `generateMinecraftManagementSources` | `generateMsmpSources`, `generateProtocolSources` | Generate typed Java from schemas (runs automatically during `build`) |
| `cleanMinecraftManagementCache` | `cleanCache` | Delete cached server JARs and extracted schemas |
| `cleanMinecraftManagementSources` | `cleanGeneratedSources` | Delete generated Java sources |

## Using Generated Client Code

Once sources are generated and the project builds, use the client:

```java
// Import generated API (version-specific)
import com.github.yannicklamprecht.mc.management.generated.v1_0_0.MinecraftManagementApi;
import com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient;

// Create and connect client
var client = new MinecraftManagementClient(
    new ObjectMapper(),
    URI.create("ws://localhost:25585"),
    "your-secret-key"
);
client.connect().join();

// Use version-specific API
var api = new MinecraftManagementApi(client);
// Now use api to call server endpoints...
```

## System Requirements

- **Java 21+**
- **Gradle 7.0+**

## Building

```bash
./gradlew build
```

## Troubleshooting

- **`packageName` property is required:** Set `packageName.set("...")` in the `minecraftManagement` block.
- **Schemas not extracted:** Run `./gradlew cleanCache extractMinecraftManagementSchemas` to re-download servers.
- **Generated sources not appearing:** Run `./gradlew cleanGeneratedSources build` to regenerate.
