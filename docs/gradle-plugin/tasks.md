# Tasks Reference

All tasks are registered under the `minecraft management` task group (`./gradlew tasks --group
"minecraft management"` to list them).

| Task | Aliases | Description |
|---|---|---|
| `extractMinecraftManagementSchemas` | `extractProtocolSchemas` | Downloads Minecraft server JARs for the configured versions, runs each one's data generator, and extracts its MSMP OpenRPC schema into `outputDir`. Caches aggressively: a Minecraft version whose schema (or lack thereof) is already cached under `cacheDir` is skipped entirely. |
| `generateMinecraftManagementSources` | `generateMsmpSources`, `generateProtocolSources` | Generates typed Java DTOs, records, enums, and API facades from the schemas in `schemasDir`. Depends on `extractMinecraftManagementSchemas`. Wired before `compileJava` once the `java` plugin is applied, but only *runs* as part of `build` under the conditions below - see [Auto-generation on `build`](#auto-generation-on-build). |
| `cleanMinecraftManagementCache` | `cleanCache` | Deletes `cacheDir` and `outputDir` - downloaded server JARs and extracted schemas. Use when you suspect the cache is stale or corrupted, or want to force a full re-extraction. |
| `cleanMinecraftManagementSources` | `cleanGeneratedSources` | Deletes `generatedSourcesDir`. Use after changing `packageName` (or any other generation-affecting property) to remove stale output from the old configuration before regenerating. |

## How they fit into a normal build

```
compileJava
    └── generateMinecraftManagementSources   (generateMsmpSources / generateProtocolSources)
            └── extractMinecraftManagementSchemas   (extractProtocolSchemas)
```

Run them explicitly whenever you want to (re)generate on demand, or inspect their output in
isolation, e.g.:

```bash
./gradlew extractMinecraftManagementSchemas   # inspect the raw OpenRPC schemas under outputDir
./gradlew generateMinecraftManagementSources  # regenerate sources without a full compile
```

## Auto-generation on `build`

`compileJava` (and therefore `build`) only *depends on* `generateMinecraftManagementSources` -
which transitively downloads Minecraft server JARs and runs their data generators - when
`generatedSourcesDir` has **no generated `.java` files yet**. Once sources have been generated once
(or checked into version control, as this repo's own `client/` module does), plain `./gradlew
build` runs will not re-trigger extraction/generation or touch the network; you regenerate
explicitly via `./gradlew generateMinecraftManagementSources` (or the `clean*` tasks) when you
want fresh output.

Override the auto-detection with the `minecraftManagement.autoGenerate` project property:

```bash
./gradlew build -PminecraftManagement.autoGenerate=true   # force the chain to run even if sources exist
./gradlew build -PminecraftManagement.autoGenerate=false  # never wire it into build, even on a first run
```

## Re-running after a configuration change

Both tasks are `@CacheableTask`s with Gradle's normal up-to-date checking, keyed off their inputs
(configured versions, `packageName`, `clientClassName`, etc.). Changing any of those and re-running
`build` regenerates only what actually needs it - you don't need to run the `clean*` tasks unless
you specifically want to discard on-disk cache/output that Gradle's own input tracking wouldn't
otherwise know to invalidate (e.g. after manually editing a cached schema file).
