# API Guide

## `MinecraftManagementServerRegistry`

The entry point for application code - inject this instead of any single session bean, since the
starter supports more than one configured server.

| Method | Returns | Use it for |
|---|---|---|
| `session(String id)` | `MinecraftManagementSession` | The common case: call MSMP methods against one configured server. Throws `NoSuchElementException` for an unknown id. |
| `get(String id)` | `ManagedServer` | The full wiring for one server - its `client()`, `session()`, `eventBridge()`, and `properties()` - when you need more than just the session. |
| `find(String id)` | `Optional<ManagedServer>` | Same as `get`, without the throw. |
| `allServers()` | `Collection<ManagedServer>` | Iterate every configured server, e.g. to build a status dashboard (see the `console` module). |
| `serverIds()` | `Set<String>` | Just the configured ids, in configuration order. |
| `isEmpty()` | `boolean` | `true` when no servers are configured at all. |

```java
MinecraftManagementSession session = registry.session("survival");
```

## `MinecraftManagementSession` - the version-agnostic API

Every method returns a `CompletableFuture` (calls) or takes a listener (notifications) - nothing
blocks the calling thread. The interface is implemented once against the union of all supported
protocol versions, so the same code works whichever protocol version a server turns out to speak.

### Server lifecycle

```java
session.getStatus();                                  // CompletableFuture<ServerStatusView>
session.save(true);                                    // flush to disk
session.stop();
session.sendSystemMessage("Server restarting soon");    // broadcast to everyone
session.sendPrivateMessage(playerNameOrUuid, "psst");    // just one player
session.kickPlayer(playerNameOrUuid, "reason");
```

### Collections: players, operators, allowlist, bans, IP bans

Each has a `get*`, and most have bulk `set*`/`add*`/`remove*`/`clear*` variants alongside the
single-item convenience methods:

```java
session.getPlayers();
session.getOperators();           session.addOperator(playerNameOrUuid, 4, false);
session.getAllowlist();           session.addToAllowlist(playerNameOrUuid);
session.getUserBans();            session.banUser(playerNameOrUuid, "reason", "console", null);
session.getIpBans();              session.banIp("203.0.113.5", "reason", "console", null);
```

### Server settings

Paired `get`/`set` methods for every MSMP server setting - difficulty, max players, view/simulation
distance, MOTD, allow-flight, autosave, and more. See `MinecraftManagementSession`'s source for the
full, current list (it grows as new protocol versions add settings).

### Game rules

```java
session.getGameRules();
session.updateGameRule("doDaylightCycle", false);
session.updateGameRule("randomTickSpeed", 3L);
```

### Notifications

Register a listener; it fires for as long as the session's underlying connection stays open:

```java
session.onPlayerJoined(player -> ...);
session.onPlayerLeft(player -> ...);
session.onServerStatus(status -> ...);
session.onOperatorAdded(operator -> ...);
session.onGameRuleUpdated(rule -> ...);
```

`onServerActivity` requires protocol 2.0.0+, and the `onWorldUpgrade*` family requires 3.1.0+ -
calling them against an older protocol throws `UnsupportedOperationException` immediately rather
than silently doing nothing. If you're not pinning `protocol-version` explicitly (see
[configuration.md](configuration.md#protocol-version-auto-detection)), guard these behind a check
of `session.protocolVersion()` unless you know every server you'll ever point this at supports
them.

**You don't usually need these directly** - the starter already turns every one of them into a
Spring `ApplicationEvent` for you. See [events.md](events.md) for the full list; prefer
`@EventListener` methods over registering session listeners yourself unless you specifically need
to bypass the event bus.

## Dropping down to a specific protocol version

`ManagedServer.client()` exposes the underlying `MinecraftManagementClient`, which has one typed
facade per generated protocol version (`v1_0_0()` through `v3_1_0()`) for when you need a method or
DTO shape that's specific to one version rather than the common subset
`MinecraftManagementSession` covers:

```java
var client = registry.get("survival").client();
var api = client.v3_1_0();
```

See the [generated code reference](../gradle-plugin/generated-code.md) for what's in each version's
facade.

## Resilience

A server that fails to connect (wrong port, offline, bad secret, ...) is logged as an error but
does not stop the application from starting or the other configured servers from connecting. It
still shows up in `registry.allServers()`/`registry.session(id)` - calls against it just fail with
`"Not connected"` until it reconnects (there's no automatic retry; a reachable server that was down
at startup needs the application restarted, or you can build your own retry against
`MinecraftManagementClient.connect()`/`isConnected()`).
