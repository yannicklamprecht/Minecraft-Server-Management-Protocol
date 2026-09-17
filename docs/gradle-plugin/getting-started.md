# Getting Started

## Prerequisites

- Java 21+
- Gradle 7.0+

## 1. Make the plugin available to your build

The plugin isn't published to the Gradle Plugin Portal or Maven Central, so a consuming project
needs to reach it another way. Pick whichever fits how you're using this project:

### Option A - composite build (recommended if you've cloned this repo)

This is exactly how this repo's own `client/` module consumes the plugin. In your consuming
project's `settings.gradle.kts`:

```kotlin
includeBuild("../path/to/minecraft-management-gradle/gradle-plugin")
```

Gradle then builds the plugin from source as part of your build - no publishing step, and changes
to the plugin are picked up immediately.

### Option B - publish to your local Maven repository

If the consuming project lives outside this repo (or you don't want a composite build), publish
the plugin once:

```bash
./gradlew :gradle-plugin:publishToMavenLocal
```

Then, in the consuming project's `settings.gradle.kts`, add `mavenLocal()` to the plugin
repositories:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

and apply it with an explicit version in `build.gradle.kts`:

```kotlin
plugins {
    id("com.github.yannicklamprecht.mc.management.schema-extractor") version "0.1.0-SNAPSHOT"
}
```

(Publishing to a real remote repository - e.g. Maven Central or a private one - works the same
way once the project has one configured; the plugin already applies the `maven-publish` plugin.)

## 2. Apply and configure the plugin

```kotlin
// build.gradle.kts
plugins {
    java
    id("com.github.yannicklamprecht.mc.management.schema-extractor")
}

minecraftManagement {
    packageName.set("com.example.myapp.msmp") // required - see configuration.md for everything else
}
```

`packageName` is the only property without a default - the build fails fast with a clear message
if it's missing.

## 3. Generate the client

```bash
./gradlew build
```

On a first run (no generated sources yet under `generatedSourcesDir`), `build` automatically
downloads the configured Minecraft server JARs (defaulting to every release from `1.21.9` onward -
see [configuration.md](configuration.md) to narrow that down), extracts their MSMP schemas,
generates typed Java sources under `src/generated/java/`, and compiles them. Once sources exist,
subsequent `build` runs compile them as-is and won't re-download/re-generate on their own - run
`./gradlew generateMinecraftManagementSources` explicitly (or pass
`-PminecraftManagement.autoGenerate=true`) to regenerate. See [tasks.md](tasks.md#auto-generation-on-build)
for the full rules and how to override them.

Everything downloaded/extracted is cached under
`.gradle/caches/minecraft-management-gradle-plugin/` (the plugin adds this to `.gitignore`
automatically), so subsequent runs are fast even across a full `clean`.

## 4. Use the generated client

The plugin generates code but doesn't add a WebSocket/HTTP dependency for you - the hand-written
transport layer (`MinecraftManagementClient` et al.) it generates against lives in this project's
`client/` module. If you're generating into a *new* module (rather than reusing `client/` as-is),
copy that transport layer (`com.github.yannicklamprecht.mc.management.transport.*` and
`com.github.yannicklamprecht.mc.management.api.*`) alongside the generated code, or depend on the
`client` module directly and just point `packageName` elsewhere for your own generated variant.

```java
import com.example.myapp.msmp.transport.MinecraftManagementClient;
import tools.jackson.databind.ObjectMapper;

var client = new MinecraftManagementClient(
        new ObjectMapper(),
        URI.create("ws://localhost:25585"),
        "your-secret-key");
client.connect().join();

// Version-agnostic facade - works across every generated protocol version:
var session = client.session();
session.getPlayers().thenAccept(players -> players.forEach(System.out::println));

// Or a specific generated protocol version's facade, if you need protocol-version-specific methods:
var api = client.v3_1_0();
```

See [generated-code.md](generated-code.md) for what exactly gets generated and how the
version-agnostic vs. version-specific APIs differ.

## Troubleshooting

- **`packageName` property is required**: set `packageName.set("...")` in the `minecraftManagement`
  block.
- **Schemas not extracted / stale**: `./gradlew cleanCache extractMinecraftManagementSchemas`
  re-downloads everything from scratch.
- **Generated sources missing or stale after a config change** (e.g. a new `packageName`):
  `./gradlew cleanGeneratedSources build` regenerates them.
- **A newer Minecraft/protocol version isn't picked up**: rerun
  `extractMinecraftManagementSchemas` - it always re-checks the version manifest; only
  already-processed Minecraft versions are skipped via the on-disk cache.
