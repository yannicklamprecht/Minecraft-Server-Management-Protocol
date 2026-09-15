# Minecraft Server Management Protocol (MSMP)

A multi-module Java 21 / Gradle project for extracting Minecraft Server Management Protocol (MSMP) OpenRPC schemas from server JARs and generating typed Java client bindings.

## Project Structure

- **`gradle-plugin/`** — Gradle plugin (`com.example.msmp.schema-extractor`) that:
  - Fetches Minecraft version manifests from Mojang
  - Downloads server JARs for specified versions
  - Executes data generation to extract protocol schemas
  - Outputs OpenRPC schemas as JSON
  
- **`client/`** — Java 21 client library providing:
  - WebSocket transport layer
  - Automatic code generation from OpenRPC schemas
  - Typed DTOs, records, enums, and API facades

## Getting Started

### 1. Configure the Plugin

Apply the plugin and configure package generation in your `build.gradle.kts`:

```kotlin
plugins {
    id("com.example.msmp.schema-extractor")
}

minecraftManagement {
    packageName.set("de.craftstuebchen.mc.management") // Required: target package for generated code
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
- **API Facades** — Version-specific API clients (e.g., `com.example.msmp.generated.v1_0_0.MinecraftManagementApi`)

## Configuration Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `packageName` | String | — | **Required.** Package name for generated sources (e.g., `com.example.msmp.generated`) |
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
import com.example.msmp.generated.v1_0_0.MinecraftManagementApi;
import com.example.msmp.transport.MinecraftManagementClient;

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
