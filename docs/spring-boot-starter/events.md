# Events Reference

Every MSMP notification is republished as a Spring `ApplicationEvent`, all extending the common
`MinecraftManagementEvent<T>` base class:

```java
public abstract class MinecraftManagementEvent<T> extends ApplicationEvent {
    public String getServerId();  // which configured server this came from
    public T getPayload();        // the notification's data, or Void for payload-less ones
}
```

Listen for a concrete subclass to only handle that notification, or for the base
`MinecraftManagementEvent<?>` type to receive every one (this is exactly what
`ActivityRecordingListener` does):

```java
@EventListener
void onPlayerJoined(PlayerJoinedEvent event) {
    MinecraftManagementSession.PlayerView player = event.getPayload();
    log.info("{} joined {}", player.name(), event.getServerId());
}
```

## Every published event

| Event | Payload | MSMP notification |
|---|---|---|
| `ServerStartedEvent` | `Void` | `minecraft:notification/server/started` |
| `ServerStoppingEvent` | `Void` | `minecraft:notification/server/stopping` |
| `ServerSavingEvent` | `Void` | `minecraft:notification/server/saving` |
| `ServerSavedEvent` | `Void` | `minecraft:notification/server/saved` |
| `ServerActivityEvent` | `Void` | `minecraft:notification/server/activity` (requires protocol 2.0.0+) |
| `ServerStatusEvent` | `ServerStatusView` | `minecraft:notification/server/status` (periodic heartbeat, if the server's `status-heartbeat-interval` is configured) |
| `PlayerJoinedEvent` | `PlayerView` | `minecraft:notification/players/joined` |
| `PlayerLeftEvent` | `PlayerView` | `minecraft:notification/players/left` |
| `OperatorAddedEvent` | `OperatorView` | `minecraft:notification/operators/added` |
| `OperatorRemovedEvent` | `OperatorView` | `minecraft:notification/operators/removed` |
| `AllowlistAddedEvent` | `PlayerView` | `minecraft:notification/allowlist/added` |
| `AllowlistRemovedEvent` | `PlayerView` | `minecraft:notification/allowlist/removed` |
| `UserBanAddedEvent` | `UserBanView` | `minecraft:notification/bans/added` |
| `UserBanRemovedEvent` | `PlayerView` | `minecraft:notification/bans/removed` |
| `IpBanAddedEvent` | `IpBanView` | `minecraft:notification/ip_bans/added` |
| `IpBanRemovedEvent` | `String` (the IP) | `minecraft:notification/ip_bans/removed` |
| `GameRuleUpdatedEvent` | `GameRuleView` | `minecraft:notification/gamerules/updated` |
| `WorldUpgradeStartedEvent` | `Void` | `minecraft:notification/world/upgrade_started` (protocol 3.1.0+ only) |
| `WorldUpgradeProgressEvent` | `BigDecimal` | `minecraft:notification/world/upgrade_progress` (protocol 3.1.0+ only) |
| `WorldUpgradeFinishedEvent` | `Void` | `minecraft:notification/world/upgrade_finished` (protocol 3.1.0+ only) |
| `WorldUpgradeFailedEvent` | `String` (failure reason) | `minecraft:notification/world/upgrade_failed` (protocol 3.1.0+ only) |

All payload record types (`PlayerView`, `OperatorView`, `UserBanView`, `IpBanView`,
`GameRuleView`, `ServerStatusView`) are nested inside `MinecraftManagementSession` - see
[api-guide.md](api-guide.md).

Every event fires regardless of whether a server's actual protocol version supports it - a
protocol that doesn't have world-upgrade notifications simply never triggers those event classes,
rather than triggering them with empty payloads.

## `ActivityRepository`

Every published event is also recorded into an `ActivityRepository`, independent of whether
anything is listening live - so a UI (or anything else) can query recent history after the fact,
not just react to events as they happen:

```java
public interface ActivityRepository {
    void record(ActivityRecord entry);
    List<ActivityRecord> findRecent(int limit);
    List<ActivityRecord> findRecentForServer(String serverId, int limit);
}

public record ActivityRecord(String serverId, String type, Object payload, Instant timestamp) {}
```

`type` is the event class's simple name with the `Event` suffix stripped (e.g. `"PlayerJoined"` for
`PlayerJoinedEvent`) - matching the first column of the table above without the suffix.

The starter registers a default `InMemoryActivityRepository` via `@ConditionalOnMissingBean` -
supply your own bean of this type to change how, or how long, activity is retained (e.g. backed by
a database). See [configuration.md](configuration.md#overriding-the-default-activityrepository).
