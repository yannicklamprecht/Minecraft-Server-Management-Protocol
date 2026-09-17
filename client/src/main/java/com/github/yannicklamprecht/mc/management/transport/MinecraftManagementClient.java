package com.github.yannicklamprecht.mc.management.transport;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class MinecraftManagementClient implements WebSocket.Listener, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftManagementClient.class);

    private final ObjectMapper mapper;
    private final HttpClient httpClient;
    private final URI uri;
    private final String secret;
    private final AtomicLong requestIds = new AtomicLong();
    private final Map<Long, PendingRequest<?>> pending = new ConcurrentHashMap<>();
    private final Map<String, NotificationRegistration<?>> notifications = new ConcurrentHashMap<>();
    private final StringBuilder incoming = new StringBuilder();
    /**
     * {@link WebSocket} only supports one outstanding send operation (text or close) at a time;
     * a second concurrent send throws {@code IllegalStateException("Send pending")}. Since this
     * client is typically shared across threads (e.g. a singleton Spring bean handling concurrent
     * HTTP requests), every outgoing frame is chained onto this future so sends happen one at a time.
     */
    private final Object sendLock = new Object();
    private CompletableFuture<?> lastSend = CompletableFuture.completedFuture(null);
    private volatile WebSocket webSocket;

    public MinecraftManagementClient(ObjectMapper mapper, URI uri, String secret) {
        this.mapper = mapper;
        this.uri = uri;
        this.secret = secret;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public CompletableFuture<Void> connect() {
        LOGGER.info("Connecting to Minecraft management server at {}", uri);
        return httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer " + secret)
                .subprotocols("minecraft-v1")
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(uri, this)
                .thenAccept(ws -> this.webSocket = ws);
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public <T> T api(java.util.function.Function<MinecraftManagementClient, T> apiFactory) {
        return apiFactory.apply(this);
    }

    /**
     * Unified, version-agnostic session abstraction that automatically works across all
     * protocol versions (1.0.0 through {@link com.github.yannicklamprecht.mc.management.ProtocolVersions#LATEST_PROTOCOL_VERSION}).
     * Speaks {@link com.github.yannicklamprecht.mc.management.ProtocolVersions#LATEST_PROTOCOL_VERSION} until
     * corrected by {@link #detectProtocolVersion()} (see there for auto-detecting the version this
     * server actually speaks instead of assuming the latest).
     */
    public com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession session() {
        return new com.github.yannicklamprecht.mc.management.api.DefaultMinecraftManagementSession(this, com.github.yannicklamprecht.mc.management.ProtocolVersions.LATEST_PROTOCOL_VERSION);
    }

    public com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession session(String protocolVersion) {
        return new com.github.yannicklamprecht.mc.management.api.DefaultMinecraftManagementSession(this, protocolVersion);
    }

    /**
     * Detects the MSMP protocol version this server actually speaks, so callers don't need to
     * configure it up front. Requires an open connection (see {@link #connect()}).
     * <p>
     * There is no dedicated "get MSMP version" RPC method, so this works by calling
     * {@code minecraft:server/status} (supported by every protocol version) and mapping its
     * {@code version.name} (the Minecraft version, e.g. {@code "1.21.11"}) through the generated
     * {@link com.github.yannicklamprecht.mc.management.ProtocolVersions#MINECRAFT_VERSION_TO_PROTOCOL_VERSION}
     * table - regenerated from the real protocol schemas by the schema-extractor Gradle plugin, so
     * rerunning {@code extractMinecraftManagementSchemas}/{@code generateMinecraftManagementSources}
     * is enough to pick up newer Minecraft/protocol versions without any code changes here.
     * <p>
     * If the reported Minecraft version isn't in that table (older than {@code minMinecraftVersion},
     * or newer than the last time the schemas were regenerated), falls back to
     * {@link com.github.yannicklamprecht.mc.management.ProtocolVersions#LATEST_PROTOCOL_VERSION} and logs a
     * warning, rather than failing outright.
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<String> detectProtocolVersion() {
        return call("minecraft:server/status", new TypeReference<Map<String, Object>>() {})
                .thenApply(status -> {
                    Object versionObj = status == null ? null : status.get("version");
                    String minecraftVersion = versionObj instanceof Map<?, ?> version && version.get("name") != null
                            ? String.valueOf(version.get("name"))
                            : null;
                    return com.github.yannicklamprecht.mc.management.ProtocolVersions.protocolVersionForMinecraftVersion(minecraftVersion)
                            .orElseGet(() -> {
                                LOGGER.warn("Could not map Minecraft version '{}' reported by the server to a known MSMP " +
                                                "protocol version; falling back to the latest known protocol {}. Rerun " +
                                                "extractMinecraftManagementSchemas/generateMinecraftManagementSources to pick up newer versions.",
                                        minecraftVersion, com.github.yannicklamprecht.mc.management.ProtocolVersions.LATEST_PROTOCOL_VERSION);
                                return com.github.yannicklamprecht.mc.management.ProtocolVersions.LATEST_PROTOCOL_VERSION;
                            });
                });
    }

    public com.github.yannicklamprecht.mc.management.v1_0_0.MinecraftManagementApi v1_0_0() {
        return new com.github.yannicklamprecht.mc.management.v1_0_0.MinecraftManagementApi(this);
    }

    public com.github.yannicklamprecht.mc.management.v1_0_0.MinecraftManagementNotifications notificationsV1_0_0() {
        return new com.github.yannicklamprecht.mc.management.v1_0_0.MinecraftManagementNotifications(this);
    }

    public com.github.yannicklamprecht.mc.management.v2_0_0.MinecraftManagementApi v2_0_0() {
        return new com.github.yannicklamprecht.mc.management.v2_0_0.MinecraftManagementApi(this);
    }

    public com.github.yannicklamprecht.mc.management.v2_0_0.MinecraftManagementNotifications notificationsV2_0_0() {
        return new com.github.yannicklamprecht.mc.management.v2_0_0.MinecraftManagementNotifications(this);
    }

    public com.github.yannicklamprecht.mc.management.v3_0_0.MinecraftManagementApi v3_0_0() {
        return new com.github.yannicklamprecht.mc.management.v3_0_0.MinecraftManagementApi(this);
    }

    public com.github.yannicklamprecht.mc.management.v3_0_0.MinecraftManagementNotifications notificationsV3_0_0() {
        return new com.github.yannicklamprecht.mc.management.v3_0_0.MinecraftManagementNotifications(this);
    }

    public com.github.yannicklamprecht.mc.management.v3_1_0.MinecraftManagementApi v3_1_0() {
        return new com.github.yannicklamprecht.mc.management.v3_1_0.MinecraftManagementApi(this);
    }

    public com.github.yannicklamprecht.mc.management.v3_1_0.MinecraftManagementNotifications notificationsV3_1_0() {
        return new com.github.yannicklamprecht.mc.management.v3_1_0.MinecraftManagementNotifications(this);
    }

    public <R> CompletableFuture<R> call(String method, TypeReference<R> resultType) {
        return call(method, null, resultType);
    }

    public <R> CompletableFuture<R> call(String method, Object params, Class<R> resultType) {
        return call(method, params, mapper.getTypeFactory().constructType(resultType));
    }

    public <R> CompletableFuture<R> call(String method, Object params, TypeReference<R> resultType) {
        return call(method, params, mapper.getTypeFactory().constructType(resultType));
    }


    private <R> CompletableFuture<R> call(String method, Object params, JavaType resultType) {
        if (webSocket == null) return CompletableFuture.failedFuture(new IllegalStateException("Not connected"));
        long id = requestIds.incrementAndGet();
        CompletableFuture<R> future = new CompletableFuture<>();
        pending.put(id, new PendingRequest<>(resultType, future));
        try {
            String payload = mapper.writeValueAsString(new JsonRpcRequest<>(id, method, params));
            LOGGER.debug("Sending JSON-RPC request id={} method={}", id, method);
            enqueueSend(() -> webSocket.sendText(payload, true)).whenComplete((ignored, error) -> {
                if (error != null) {
                    PendingRequest<?> p = pending.remove(id);
                    if (p != null) p.future().completeExceptionally(error);
                }
            });
        } catch (Exception e) {
            pending.remove(id);
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Chains {@code send} onto the previous outstanding send so at most one is ever in flight,
     * regardless of how many threads call {@link #call} concurrently.
     */
    private CompletableFuture<?> enqueueSend(java.util.function.Supplier<CompletableFuture<WebSocket>> send) {
        synchronized (sendLock) {
            CompletableFuture<?> next = lastSend.handle((ignoredResult, ignoredError) -> null)
                    .thenCompose(ignored -> send.get());
            lastSend = next;
            return next;
        }
    }

    public void registerNotification(String method, Runnable listener) {
        notifications.put(method, new NotificationRegistration<>(
                mapper.getTypeFactory().constructType(new TypeReference<Object>() {}),
                ignored -> listener.run()
        ));
    }

    public <T> void registerNotification(String method, TypeReference<T> type, Consumer<T> listener) {
        notifications.put(method, new NotificationRegistration<>(mapper.getTypeFactory().constructType(type), listener));
    }

    public <T> void registerNotificationProperty(String method, String propertyName, TypeReference<T> type, Consumer<T> listener) {
        JavaType targetType = mapper.getTypeFactory().constructType(type);
        notifications.put(method, new NotificationRegistration<>(
                mapper.getTypeFactory().constructType(JsonNode.class),
                params -> {
                    JsonNode values = (JsonNode) params;
                    JsonNode value;
                    if (values != null && values.isArray() && values.size() == 1) {
                        value = values.get(0);
                    } else if (values != null && values.isObject() && values.has(propertyName)) {
                        value = values.get(propertyName);
                    } else {
                        throw new IllegalArgumentException("Expected one positional parameter or named parameter '" + propertyName + "'");
                    }
                    T converted = mapper.convertValue(value, targetType);
                    listener.accept(converted);
                }
        ));
    }

    @Override public void onOpen(WebSocket webSocket) { LOGGER.info("WebSocket opened"); webSocket.request(1); }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        incoming.append(data);
        if (last) {
            String message = incoming.toString();
            incoming.setLength(0);
            processMessage(message);
        }
        webSocket.request(1);
        return null;
    }

    private void processMessage(String raw) {
        try {
            JsonRpcMessageHeader header = mapper.readValue(raw, JsonRpcMessageHeader.class);
            if (header.id() != null) processResponse(header.id(), raw);
            else if (header.method() != null) processNotification(header.method(), raw);
            else LOGGER.warn("Ignoring JSON-RPC message without id or method");
        } catch (Exception e) {
            LOGGER.error("Could not deserialize incoming JSON-RPC message", e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processResponse(long id, String raw) throws Exception {
        PendingRequest p = pending.remove(id);
        if (p == null) { LOGGER.warn("Response for unknown request id={}", id); return; }
        JavaType responseType = mapper.getTypeFactory().constructParametricType(JsonRpcResponse.class, p.resultType());
        JsonRpcResponse<?> response = mapper.readValue(raw, responseType);
        if (response.isError()) p.future().completeExceptionally(new JsonRpcException(response.error()));
        else p.future().complete(response.result());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processNotification(String method, String raw) throws Exception {
        NotificationRegistration reg = notifications.get(method);
        if (reg == null) { LOGGER.debug("Ignoring unregistered notification {}", method); return; }
        JavaType type = mapper.getTypeFactory().constructParametricType(JsonRpcNotification.class, reg.paramsType());
        JsonRpcNotification<?> notification = mapper.readValue(raw, type);
        reg.consumer().accept(notification.params());
    }

    @Override public void onError(WebSocket webSocket, Throwable error) { LOGGER.error("WebSocket failed", error); this.webSocket = null; failAll(error); }
    @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        LOGGER.info("Connection closed: code={} reason={}", statusCode, reason);
        this.webSocket = null;
        failAll(new IllegalStateException("WebSocket closed: " + statusCode + " " + reason));
        return null;
    }
    private void failAll(Throwable t) { pending.values().forEach(p -> p.future().completeExceptionally(t)); pending.clear(); }

    /**
     * Whether this client currently has an open connection. Backed by the same state {@link #call}
     * uses to fail fast with "Not connected" - cleared as soon as the WebSocket closes or errors,
     * so this reflects the live connection state rather than "connected at some point".
     */
    public boolean isConnected() {
        return webSocket != null;
    }

    @Override
    public void close() {
        WebSocket ws = webSocket;
        webSocket = null;
        if (ws != null) enqueueSend(() -> ws.sendClose(WebSocket.NORMAL_CLOSURE, "Client shutdown"));
    }

    private record PendingRequest<T>(JavaType resultType, CompletableFuture<T> future) {}
    private record NotificationRegistration<T>(JavaType paramsType, Consumer<T> consumer) {}
}
