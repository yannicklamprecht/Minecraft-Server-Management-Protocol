package com.github.yannicklamprecht.mc.management.console.web;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.console.config.ConsoleUiProperties;
import com.github.yannicklamprecht.mc.management.console.config.OperatorNotificationMode;
import com.github.yannicklamprecht.mc.management.console.web.dto.KickRequest;
import com.github.yannicklamprecht.mc.management.console.web.dto.MessageRequest;
import com.github.yannicklamprecht.mc.management.console.web.dto.OperatorRequest;
import com.github.yannicklamprecht.mc.management.console.web.dto.PlayerNameRequest;
import com.github.yannicklamprecht.mc.management.console.web.dto.SaveRequest;
import com.github.yannicklamprecht.mc.management.console.web.dto.ServerSummary;
import com.github.yannicklamprecht.mc.management.console.web.dto.UserBanRequest;
import com.github.yannicklamprecht.mc.management.spring.MinecraftManagementClientLifecycle;
import com.github.yannicklamprecht.mc.management.spring.MinecraftManagementServerRegistry;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRecord;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * JSON API consumed by the plain-JS frontend (see {@code static/app.js}). The application can be
 * configured to talk to several MSMP servers at once (see {@code minecraft.management.servers.*}),
 * so every endpoint except the server listing is scoped under {@code /api/servers/{serverId}}.
 * Every method delegates straight to the version-agnostic {@link MinecraftManagementSession} for
 * that server; Spring MVC resolves the returned {@link CompletableFuture} asynchronously so no
 * request thread blocks on the websocket round-trip to the Minecraft server.
 */
@RestController
@RequestMapping("/api/servers")
public class ManagementApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementApiController.class);
    private static final int DEFAULT_ACTIVITY_LIMIT = 200;

    private final MinecraftManagementServerRegistry registry;
    private final MinecraftManagementClientLifecycle lifecycle;
    private final ActivityRepository activityRepository;
    private final ConsoleUiProperties uiProperties;

    /**
     * Kicking a player is not idempotent on the Minecraft server: a second {@code players/kick}
     * for a connection that's already being torn down throws {@code IllegalStateException: Already
     * retired} server-side. That can happen from a single browser tab (a UI re-render racing with
     * an in-flight request), from two tabs/clients open against this same console, or from a
     * direct API call - so the guard belongs here, shared across every caller, rather than only in
     * the frontend's own in-flight tracking. Keyed by "serverId playerId" since the same
     * player name can exist independently on two different configured servers.
     */
    private final Set<String> kicksInFlight = ConcurrentHashMap.newKeySet();

    public ManagementApiController(MinecraftManagementServerRegistry registry, MinecraftManagementClientLifecycle lifecycle,
                                    ActivityRepository activityRepository, ConsoleUiProperties uiProperties) {
        this.registry = registry;
        this.lifecycle = lifecycle;
        this.activityRepository = activityRepository;
        this.uiProperties = uiProperties;
    }

    /**
     * For each configured server, reports whether it's currently connected and, if so, fetches its
     * live status to read off the Minecraft version it's running - a fresh check every call rather
     * than a cached value, so the sidebar's online/offline display can't drift from reality. A
     * server that's connected but whose status call itself fails (e.g. mid-disconnect) is reported
     * offline rather than failing the whole listing.
     */
    @GetMapping
    public CompletableFuture<List<ServerSummary>> servers() {
        List<CompletableFuture<ServerSummary>> summaries = registry.allServers().stream()
                .map(this::summarize)
                .toList();
        return CompletableFuture.allOf(summaries.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> summaries.stream().map(CompletableFuture::join).toList());
    }

    private CompletableFuture<ServerSummary> summarize(MinecraftManagementServerRegistry.ManagedServer server) {
        String protocolVersion = server.session().protocolVersion();
        String url = server.properties().getUrl().toString();
        if (!server.client().isConnected()) {
            return CompletableFuture.completedFuture(new ServerSummary(server.id(), false, null, protocolVersion, url));
        }
        return server.session().getStatus()
                .thenApply(status -> new ServerSummary(server.id(), true,
                        status.version() != null ? status.version().name() : null, protocolVersion, url))
                .exceptionally(error -> new ServerSummary(server.id(), false, null, protocolVersion, url));
    }

    /**
     * Manually retries connecting a server that isn't currently connected - e.g. the "retry"
     * button on the console's offline-troubleshooting panel. Always resolves with 200 and that
     * server's current summary rather than an error status: a still-failed retry is a normal
     * outcome to report (the frontend just re-renders as still offline), not a server error.
     * A no-op if the server is already connected.
     */
    @PostMapping("/{serverId}/reconnect")
    public CompletableFuture<ServerSummary> reconnect(@PathVariable String serverId) {
        MinecraftManagementServerRegistry.ManagedServer server = registry.get(serverId);
        return lifecycle.reconnect(serverId)
                .exceptionally(error -> null) // connect failed - summarize() below reports it as offline
                .thenCompose(ignored -> summarize(server));
    }

    /**
     * The activity recorded for this server so far, oldest first. Lets the frontend hydrate its
     * activity feed with history from before the page's {@code /api/events} subscription was
     * opened (e.g. right after selecting this server, or after a reload), instead of only ever
     * showing events received live from that point on.
     */
    @GetMapping("/{serverId}/activity")
    public List<ActivityRecord> activity(@PathVariable String serverId,
                                          @RequestParam(defaultValue = "" + DEFAULT_ACTIVITY_LIMIT) int limit) {
        return activityRepository.findRecentForServer(serverId, limit);
    }

    @GetMapping("/{serverId}/status")
    public CompletableFuture<MinecraftManagementSession.ServerStatusView> status(@PathVariable String serverId) {
        return session(serverId).getStatus();
    }

    @PostMapping("/{serverId}/save")
    public CompletableFuture<ResponseEntity<Void>> save(@PathVariable String serverId, @RequestBody SaveRequest request) {
        return session(serverId).save(request.flush()).thenApply(ignored -> ResponseEntity.ok().build());
    }

    /**
     * Broadcasts, or - when {@code playerNameOrUuid} is given - privately sends, a system chat
     * message. Unlike saves, kicks, or operator/allowlist/ban changes, MSMP has no notification for
     * an outgoing system message - so unless it's recorded here, it never enters the activity
     * history at all (it would only ever have been the sending tab's own transient log line).
     */
    @PostMapping("/{serverId}/messages")
    public CompletableFuture<ResponseEntity<Void>> sendMessage(@PathVariable String serverId, @RequestBody MessageRequest request) {
        String target = request.playerNameOrUuid();
        if (target != null && !target.isBlank()) {
            return session(serverId).sendPrivateMessage(target, request.message())
                    .thenApply(ignored -> {
                        activityRepository.record(new ActivityRecord(serverId, "PrivateMessageSent",
                                target + ": " + request.message(), Instant.now()));
                        return ResponseEntity.ok().<Void>build();
                    });
        }
        return session(serverId).sendSystemMessage(request.message())
                .thenApply(ignored -> {
                    activityRepository.record(new ActivityRecord(serverId, "MessageSent", request.message(), Instant.now()));
                    return ResponseEntity.ok().build();
                });
    }

    @GetMapping("/{serverId}/players")
    public CompletableFuture<List<MinecraftManagementSession.PlayerView>> players(@PathVariable String serverId) {
        return session(serverId).getPlayers();
    }

    @PostMapping("/{serverId}/players/kick")
    public CompletableFuture<ResponseEntity<Object>> kick(@PathVariable String serverId, @RequestBody KickRequest request) {
        String kickKey = serverId + " " + request.playerNameOrUuid();
        if (!kicksInFlight.add(kickKey)) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A kick for this player is already in progress")));
        }
        try {
            return withOperatorNotification(serverId, request.playerNameOrUuid(), request.notifyOperators(),
                    displayName(request.playerNameOrUuid(), request.playerDisplayName()) + " was kicked" + reasonSuffix(request.reason()),
                    s -> s.kickPlayer(request.playerNameOrUuid(), request.reason()))
                    .<ResponseEntity<Object>>thenApply(ignored -> ResponseEntity.ok().build())
                    .whenComplete((result, error) -> kicksInFlight.remove(kickKey));
        } catch (RuntimeException e) {
            // session.kickPlayer failed before it could even return a future (e.g. a malformed
            // request) - without this, kickKey would stay marked in-flight forever.
            kicksInFlight.remove(kickKey);
            throw e;
        }
    }

    @GetMapping("/{serverId}/operators")
    public CompletableFuture<List<MinecraftManagementSession.OperatorView>> operators(@PathVariable String serverId) {
        return session(serverId).getOperators();
    }

    @PostMapping("/{serverId}/operators")
    public CompletableFuture<ResponseEntity<Void>> addOperator(@PathVariable String serverId, @RequestBody OperatorRequest request) {
        return withOperatorNotification(serverId, request.playerNameOrUuid(), request.notifyOperators(),
                displayName(request.playerNameOrUuid(), request.playerDisplayName()) + " was made an operator.",
                s -> s.addOperator(request.playerNameOrUuid(), request.permissionLevel(), request.bypassesPlayerLimit()))
                .thenApply(ignored -> ResponseEntity.ok().build());
    }

    @DeleteMapping("/{serverId}/operators/{playerNameOrUuid}")
    public CompletableFuture<ResponseEntity<Void>> removeOperator(@PathVariable String serverId, @PathVariable String playerNameOrUuid,
                                                                    @RequestParam(required = false) Boolean notifyOperators,
                                                                    @RequestParam(required = false) String playerDisplayName) {
        return withOperatorNotification(serverId, playerNameOrUuid, notifyOperators,
                displayName(playerNameOrUuid, playerDisplayName) + " is no longer an operator.",
                s -> s.removeOperator(playerNameOrUuid))
                .thenApply(ignored -> ResponseEntity.ok().build());
    }

    @GetMapping("/{serverId}/allowlist")
    public CompletableFuture<List<MinecraftManagementSession.PlayerView>> allowlist(@PathVariable String serverId) {
        return session(serverId).getAllowlist();
    }

    @PostMapping("/{serverId}/allowlist")
    public CompletableFuture<ResponseEntity<Void>> addToAllowlist(@PathVariable String serverId, @RequestBody PlayerNameRequest request) {
        return session(serverId).addToAllowlist(request.playerNameOrUuid()).thenApply(ignored -> ResponseEntity.ok().build());
    }

    @DeleteMapping("/{serverId}/allowlist/{playerNameOrUuid}")
    public CompletableFuture<ResponseEntity<Void>> removeFromAllowlist(@PathVariable String serverId, @PathVariable String playerNameOrUuid) {
        return session(serverId).removeFromAllowlist(playerNameOrUuid).thenApply(ignored -> ResponseEntity.ok().build());
    }

    @GetMapping("/{serverId}/bans/users")
    public CompletableFuture<List<MinecraftManagementSession.UserBanView>> userBans(@PathVariable String serverId) {
        return session(serverId).getUserBans();
    }

    @PostMapping("/{serverId}/bans/users")
    public CompletableFuture<ResponseEntity<Void>> banUser(@PathVariable String serverId, @RequestBody UserBanRequest request) {
        return withOperatorNotification(serverId, request.playerNameOrUuid(), request.notifyOperators(),
                displayName(request.playerNameOrUuid(), request.playerDisplayName()) + " was banned" + reasonSuffix(request.reason()),
                s -> s.banUser(request.playerNameOrUuid(), request.reason(), request.source(), request.expires()))
                .thenApply(ignored -> ResponseEntity.ok().build());
    }

    @DeleteMapping("/{serverId}/bans/users/{playerNameOrUuid}")
    public CompletableFuture<ResponseEntity<Void>> unbanUser(@PathVariable String serverId, @PathVariable String playerNameOrUuid) {
        return session(serverId).unbanUser(playerNameOrUuid).thenApply(ignored -> ResponseEntity.ok().build());
    }

    private MinecraftManagementSession session(String serverId) {
        return registry.session(serverId);
    }

    /**
     * Runs {@code action} against the server's session and, when
     * {@code console.ui.operator-notifications} calls for it (see {@link OperatorNotificationMode#shouldNotify}),
     * privately messages every other currently-online operator {@code notificationMessage}
     * afterward - "other" meaning every operator except {@code targetPlayerNameOrUuid} itself, so a
     * player is never notified about their own kick/ban/op change. The operator list is snapshotted
     * <em>before</em> {@code action} runs, so removing an operator still correctly excludes them
     * (by the time the action completes they're no longer in a freshly-fetched list, but they were
     * the one performed on regardless).
     * <p>
     * A failure to send one or all of the notifications is logged and does not fail the action
     * itself - the moderation action already succeeded by that point.
     */
    private CompletableFuture<Void> withOperatorNotification(String serverId, String targetPlayerNameOrUuid, Boolean requestedNotify,
                                                               String notificationMessage,
                                                               Function<MinecraftManagementSession, CompletableFuture<Void>> action) {
        MinecraftManagementSession session = session(serverId);
        boolean notify = uiProperties.getOperatorNotifications().shouldNotify(requestedNotify);
        CompletableFuture<List<MinecraftManagementSession.OperatorView>> operatorsBefore = notify
                ? session.getOperators()
                : CompletableFuture.completedFuture(List.of());
        return operatorsBefore.thenCompose(operatorsSnapshot -> action.apply(session).thenCompose(ignored -> {
            if (!notify) return CompletableFuture.completedFuture(null);
            return notifyOtherOperators(session, operatorsSnapshot, targetPlayerNameOrUuid, notificationMessage);
        }));
    }

    private CompletableFuture<Void> notifyOtherOperators(MinecraftManagementSession session,
                                                           List<MinecraftManagementSession.OperatorView> operators,
                                                           String excludePlayerNameOrUuid, String message) {
        List<CompletableFuture<Boolean>> sends = operators.stream()
                .map(MinecraftManagementSession.OperatorView::player)
                .filter(player -> !isSamePlayer(player, excludePlayerNameOrUuid))
                .map(player -> session.sendPrivateMessage(playerIdentifier(player), message))
                .toList();
        return CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new))
                .exceptionally(error -> {
                    LOGGER.warn("Failed to notify one or more operators: {}", error.getMessage());
                    return null;
                });
    }

    private static boolean isSamePlayer(MinecraftManagementSession.PlayerView player, String nameOrUuid) {
        return nameOrUuid != null && (nameOrUuid.equalsIgnoreCase(player.id()) || nameOrUuid.equalsIgnoreCase(player.name()));
    }

    private static String playerIdentifier(MinecraftManagementSession.PlayerView player) {
        return player.id() != null ? player.id() : player.name();
    }

    private static String reasonSuffix(String reason) {
        return reason != null && !reason.isBlank() ? " (" + reason + ")." : ".";
    }

    /**
     * The name to use in an operator notification: the caller-supplied display name if given
     * (e.g. the dashboard's player list already knows the player's name, even when
     * {@code playerNameOrUuid} - what's actually sent to identify the player to the Minecraft
     * server - is a UUID), falling back to {@code playerNameOrUuid} itself otherwise.
     */
    private static String displayName(String playerNameOrUuid, String playerDisplayName) {
        return playerDisplayName != null && !playerDisplayName.isBlank() ? playerDisplayName : playerNameOrUuid;
    }
}
