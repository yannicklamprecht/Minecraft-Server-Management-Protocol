# Configuration Reference

All properties live in the `minecraftManagement { ... }` extension block.

```kotlin
minecraftManagement {
    packageName.set("com.example.myapp.msmp")     // Required
    onlyReleases.set(true)                          // default: true
    minMinecraftVersion.set("1.21.9")                // default: 1.21.9 (when MSMP was introduced)
    // versions.set(listOf("1.21.9", "1.21.10"))     // optional: pin an explicit set instead
}
```

| Property | Type | Default | Description |
|---|---|---|---|
| `packageName` | `Property<String>` | *(none - required)* | Root Java package for every generated class. Intentionally has no default, so a build fails fast with a clear message instead of silently generating into the wrong place. |
| `onlyReleases` | `Property<Boolean>` | `true` | When `true`, only stable release versions are considered (snapshots/release-candidates excluded) unless `versions` is set explicitly. |
| `minMinecraftVersion` | `Property<String>` | `1.21.9` | The oldest Minecraft version to check, scanning newest-to-oldest from the version manifest. Ignored when `versions` is non-empty. `1.21.9` is the version MSMP was introduced in. |
| `versions` | `ListProperty<String>` | *(empty)* | An explicit list of Minecraft versions to process, overriding `onlyReleases`/`minMinecraftVersion` entirely. Use this to pin exactly which versions/protocols you want generated. |
| `manifestUrl` | `Property<String>` | Mojang's official `version_manifest_v2.json` | Override to point at a mirror, or a custom manifest for testing. |
| `javaExecutable` | `Property<String>` | *(auto-detected)* | Path to a `java` executable. Each Minecraft version's data generator needs a specific Java major version to run under; left unset, the plugin locates a suitable one automatically (see `JavaRuntimeLocator`). Set this to force a specific toolchain. |
| `outputDir` | `DirectoryProperty` | `.gradle/caches/minecraft-management-gradle-plugin/protocol-schemas` | Where extracted OpenRPC schemas are written, one subdirectory per **protocol** version (e.g. `3.1.0/openrpc.json`) - several Minecraft versions that speak the same protocol version share one file. |
| `cacheDir` | `DirectoryProperty` | `.gradle/caches/minecraft-management-gradle-plugin/minecraft-servers` | Where downloaded server JARs (and their extracted, per-**Minecraft**-version schema cache) are kept, so re-running extraction doesn't redownload/rerun the data generator for a version already processed. |
| `schemasDir` | `DirectoryProperty` | same as `outputDir` | Input directory the *generation* step reads schemas from. Only needs to differ from `outputDir` if you're extracting schemas in one project and generating code from them in another. |
| `generatedSourcesDir` | `DirectoryProperty` | `src/generated/java` (relative to the project) | Where generated `.java` files are written. Automatically added to the `main` source set's `java` source directories when the `java` plugin is applied. |
| `clientClassName` | `Property<String>` | `com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient` | Fully-qualified name of the hand-written client class generated API facades take as a constructor parameter. Only needs changing if you've moved/renamed that class relative to this project's own copy - see [generated-code.md](generated-code.md). |

Additionally, the `minecraftManagement.autoGenerate` **project property** (not an extension
property - pass it with `-P`, it isn't set in the `minecraftManagement { ... }` block) controls
whether `build` auto-triggers extraction/generation; see
[Auto-generation on `build`](tasks.md#auto-generation-on-build) in the tasks reference.

## Caching and `.gitignore`

The extraction task ensures `outputDir` has an entry in the project's `.gitignore` the first time
it runs, if not already present - relevant mainly if you point it somewhere outside `.gradle/`,
since a typical project's own top-level `.gitignore` already excludes `.gradle/` (and so, by
extension, the default `cacheDir` too) regardless. `generatedSourcesDir` is **not** touched
automatically - whether generated sources are checked in is left entirely up to you. This
repository's own `client/` module deliberately checks its generated sources in, so the module
builds without requiring the extraction/generation step to have run first; that's a project-level
choice, not something the plugin enforces either way.

## Picking versions deliberately

If you only care about the protocol versions your target servers actually speak, prefer an
explicit `versions` list over widening `minMinecraftVersion` - each additional Minecraft version
means another server JAR download and a data-generator run (slow, and each one needs a matching
Java major version available). This project's own `client/build.gradle.kts` only sets
`packageName` and relies on the default `minMinecraftVersion`, since it intentionally tracks every
release since MSMP's introduction.
