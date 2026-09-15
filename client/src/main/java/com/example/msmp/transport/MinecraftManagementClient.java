package com.example.msmp.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public com.example.msmp.generated.v1_0_0.MinecraftManagementApi v1_0_0() {
        return new com.example.msmp.generated.v1_0_0.MinecraftManagementApi(this);
    }

    public com.example.msmp.generated.v1_0_0.MinecraftManagementNotifications notificationsV1_0_0() {
        return new com.example.msmp.generated.v1_0_0.MinecraftManagementNotifications(this);
    }

    public com.example.msmp.generated.v2_0_0.MinecraftManagementApi v2_0_0() {
        return new com.example.msmp.generated.v2_0_0.MinecraftManagementApi(this);
    }

    public com.example.msmp.generated.v2_0_0.MinecraftManagementNotifications notificationsV2_0_0() {
        return new com.example.msmp.generated.v2_0_0.MinecraftManagementNotifications(this);
    }

    public com.example.msmp.generated.v3_0_0.MinecraftManagementApi v3_0_0() {
        return new com.example.msmp.generated.v3_0_0.MinecraftManagementApi(this);
    }

    public com.example.msmp.generated.v3_0_0.MinecraftManagementNotifications notificationsV3_0_0() {
        return new com.example.msmp.generated.v3_0_0.MinecraftManagementNotifications(this);
    }

    public com.example.msmp.generated.v3_1_0.MinecraftManagementApi v3_1_0() {
        return new com.example.msmp.generated.v3_1_0.MinecraftManagementApi(this);
    }

    public com.example.msmp.generated.v3_1_0.MinecraftManagementNotifications notificationsV3_1_0() {
        return new com.example.msmp.generated.v3_1_0.MinecraftManagementNotifications(this);
    }

    public <R> CompletableFuture<R> call(String method, TypeReference<R> resultType) {
        return call(method, Map.of(), resultType);
    }

    public <R> CompletableFuture<R> call(String method, Object params, Class<R> resultType) {
        return call(method, params, mapper.getTypeFactory().constructType(resultType));
    }

    public <R> CompletableFuture<R> call(String method, Object params, TypeReference<R> resultType) {
        return call(method, params, mapper.getTypeFactory().constructType(resultType));
    }

    @SuppressWarnings("unchecked")
    private <R> CompletableFuture<R> call(String method, Object params, JavaType resultType) {
        if (webSocket == null) return CompletableFuture.failedFuture(new IllegalStateException("Not connected"));
        long id = requestIds.incrementAndGet();
        CompletableFuture<R> future = new CompletableFuture<>();
        pending.put(id, new PendingRequest<>(resultType, future));
        try {
            String payload = mapper.writeValueAsString(new JsonRpcRequest<>(id, method, params));
            LOGGER.debug("Sending JSON-RPC request id={} method={}", id, method);
            webSocket.sendText(payload, true).whenComplete((ignored, error) -> {
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
                mapper.getTypeFactory().constructType(new TypeReference<Map<String, Object>>() {}),
                params -> {
                    if (params instanceof Map<?, ?> map) {
                        Object propValue = map.get(propertyName);
                        T converted = mapper.convertValue(propValue, targetType);
                        listener.accept(converted);
                    }
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

    @Override public void onError(WebSocket webSocket, Throwable error) { LOGGER.error("WebSocket failed", error); failAll(error); }
    @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        LOGGER.info("Connection closed: code={} reason={}", statusCode, reason);
        failAll(new IllegalStateException("WebSocket closed: " + statusCode + " " + reason));
        return null;
    }
    private void failAll(Throwable t) { pending.values().forEach(p -> p.future().completeExceptionally(t)); pending.clear(); }
    @Override public void close() { if (webSocket != null) webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client shutdown"); }

    private record PendingRequest<T>(JavaType resultType, CompletableFuture<T> future) {}
    private record NotificationRegistration<T>(JavaType paramsType, Consumer<T> consumer) {}
}
