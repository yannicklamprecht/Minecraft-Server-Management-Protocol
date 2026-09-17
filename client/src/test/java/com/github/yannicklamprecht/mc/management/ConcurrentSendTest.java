package com.github.yannicklamprecht.mc.management;

import com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link WebSocket} only allows one outstanding send operation at a time; a second concurrent
 * {@code sendText}/{@code sendClose} throws {@code IllegalStateException("Send pending")}. Since a
 * single client is commonly shared across threads (e.g. a Spring singleton bean serving concurrent
 * HTTP requests), the client must serialize its sends instead of relying on callers to do so.
 */
class ConcurrentSendTest {

    @Test
    void secondCallWaitsForFirstSendToCompleteBeforeSendingItsOwnFrame() throws Exception {
        AtomicInteger sendCount = new AtomicInteger();
        List<CompletableFuture<WebSocket>> pendingSends = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        var client = new MinecraftManagementClient(mapper, URI.create("ws://localhost:25585"), "test");
        WebSocket socket = (WebSocket) Proxy.newProxyInstance(
                WebSocket.class.getClassLoader(), new Class<?>[]{WebSocket.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendText")) {
                        sendCount.incrementAndGet();
                        CompletableFuture<WebSocket> future = new CompletableFuture<>();
                        pendingSends.add(future);
                        return future;
                    }
                    return CompletableFuture.completedFuture(proxy);
                });
        var field = MinecraftManagementClient.class.getDeclaredField("webSocket");
        field.setAccessible(true);
        field.set(client, socket);

        client.session().getStatus();
        client.session().getPlayers();

        // The second call's frame must not be sent while the first is still outstanding.
        assertEquals(1, sendCount.get());

        pendingSends.get(0).complete(socket);

        // Completing the first send unblocks the second, queued frame.
        assertEquals(2, sendCount.get());

        pendingSends.get(1).complete(socket);
    }
}
