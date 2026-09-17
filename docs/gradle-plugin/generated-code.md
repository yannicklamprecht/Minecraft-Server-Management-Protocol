# Generated Code Reference

## Directory layout

For `packageName.set("com.example.myapp.msmp")`, generation produces:

```
src/generated/java/com/example/myapp/msmp/
├── ProtocolVersions.java              # top-level, not version-scoped - see below
├── v1_0_0/
│   ├── ProtocolInfo.java              # VERSION/TITLE constants for this protocol version
│   ├── MinecraftManagementApi.java    # typed request/response facade
│   ├── MinecraftManagementNotifications.java  # typed notification-subscription facade
│   └── dto/
│       ├── Player.java, Operator.java, UserBan.java, IpBan.java, ...   # records
│       ├── GameType.java, Difficulty.java, ...                         # enums
│       └── TypedGameRule.java, UntypedGameRule.java, ...
├── v2_0_0/  (same shape)
├── v3_0_0/  (same shape)
└── v3_1_0/  (same shape)
```

One `v<protocol>` package per **MSMP protocol** version found (not per Minecraft version - e.g.
Minecraft `1.21.9` and `1.21.10` both speak protocol `1.0.0`, so they share `v1_0_0`). The package
name segment mirrors the protocol version with dots replaced by underscores.

## Version-specific API: `MinecraftManagementApi` / `MinecraftManagementNotifications`

Each protocol version gets its own strongly-typed facade, generated directly from that version's
OpenRPC schema:

```java
var api = new com.example.myapp.msmp.v3_1_0.MinecraftManagementApi(client);
api.serverStatus().thenAccept(status -> ...);          // CompletableFuture<ServerState>
api.gamerulesUpdate(...);                                // only exists where the v3.1.0 schema has it
```

`MinecraftManagementClient` (hand-written, in `com.github.yannicklamprecht.mc.management.transport`
in this project - see below) exposes a convenience accessor per generated version:
`client.v1_0_0()`, `client.v2_0_0()`, `client.v3_0_0()`, `client.v3_1_0()`, plus a
`notificationsV<version>()` counterpart for each. Use this facade when you need a method or DTO
shape that's specific to one protocol version, or want compile-time confidence about exactly what
a given server version supports.

## Version-agnostic API: `MinecraftManagementSession`

Hand-written (not generated), `MinecraftManagementSession` is a single interface implemented
against the union of all four protocol versions' actual wire behavior - most MSMP methods are
identical across versions, so one implementation (`DefaultMinecraftManagementSession`) covers
1.0.0 through 3.1.0+, with the handful of genuinely version-specific behaviors (e.g. game rule
value encoding, or that world-upgrade notifications only exist from 3.1.0 onward) handled
internally:

```java
MinecraftManagementSession session = client.session();              // latest known protocol
MinecraftManagementSession session = client.session("2.0.0");        // pin a specific one
session.getPlayers().thenAccept(players -> ...);
session.onPlayerJoined(player -> ...);
```

Prefer this for most application code - it's what the [Spring Boot starter](../spring-boot-starter/README.md) uses, and what lets a server's actual protocol version be
[auto-detected at connect time](../spring-boot-starter/configuration.md#protocol-version-auto-detection)
instead of assumed up front.

## `ProtocolVersions` - the Minecraft-version-to-protocol-version map

Generated once per run (not per protocol version) into the root of `packageName`:

```java
public final class ProtocolVersions {
    public static final List<String> SUPPORTED_PROTOCOL_VERSIONS;      // e.g. ["1.0.0", "2.0.0", "3.0.0", "3.1.0"]
    public static final String LATEST_PROTOCOL_VERSION;                 // e.g. "3.1.0"
    public static final Map<String, String> MINECRAFT_VERSION_TO_PROTOCOL_VERSION; // "1.21.11" -> "2.0.0"

    public static Optional<String> protocolVersionForMinecraftVersion(String minecraftVersion);
}
```

There's no MSMP method to ask a server "what protocol version are you speaking" directly - this
table is what makes auto-detection possible: call `minecraft:server/status` (supported by every
protocol version), read `version.name` (the Minecraft version, e.g. `"1.21.11"`) from the
response, and look it up here. `MinecraftManagementClient.detectProtocolVersion()` does exactly
this. **Regenerating (rerunning `extractMinecraftManagementSchemas` +
`generateMinecraftManagementSources`) is the only thing needed to pick up newly released
Minecraft/protocol versions** - there's no hand-maintained mapping to update.

## The hand-written layer generated code depends on

Generated API facades take a `MinecraftManagementClient` (the `clientClassName` configuration
property) in their constructor and call its `call(method, params, resultType)` /
`registerNotification(...)` methods to actually talk over the WebSocket. That transport layer,
along with `MinecraftManagementSession`/`DefaultMinecraftManagementSession`, is **not generated** -
it's hand-written in this project's `client/` module
(`com.github.yannicklamprecht.mc.management.transport.*` and
`com.github.yannicklamprecht.mc.management.api.*`) and reused as-is regardless of how many times
you regenerate.

**Important if you configure a different `packageName` than this project's own**:
`MinecraftManagementClient`'s `v1_0_0()`/`v2_0_0()`/etc. convenience methods, and
`DefaultMinecraftManagementSession`'s reference to `ProtocolVersions`, are hardcoded to
`com.github.yannicklamprecht.mc.management` (this project's own generated package) - they aren't
regenerated, so they won't resolve against a differently-named package you generate elsewhere. If
you're generating into a genuinely different package (e.g. building a standalone client, not
reusing this project's `client` module), either:

- keep `packageName` set to `com.github.yannicklamprecht.mc.management` so the existing
  transport layer's references keep resolving, or
- copy the transport layer alongside your generated code and update those specific references to
  match your own `packageName`.

Sticking with `client.session()`/`client.session(version)` (which take the protocol version as a
plain string, not a hardcoded package reference) avoids this entirely and works regardless of
`packageName`.
