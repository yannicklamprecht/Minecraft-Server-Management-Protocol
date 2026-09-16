package com.example.msmp;

import com.example.msmp.transport.MinecraftManagementClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ClientRequestTest {
    @Test
    void parameterlessCallsOmitParamsAcrossSessionAndGeneratedApis() throws Exception {
        List<JsonNode> requests = new ArrayList<>();
        try (var client = recordingClient(requests)) {
            client.session().getStatus();
            client.session().stop();
            client.session().getPlayers();
            client.v1_0_0().serverStatus();
            client.v2_0_0().serverStatus();
            client.v3_0_0().serverStatus();
            client.v3_1_0().serverStatus();

            assertEquals(7, requests.size());
            for (JsonNode request : requests) {
                assertEquals("2.0", request.path("jsonrpc").asText());
                assertFalse(request.has("params"), request.toString());
            }
            assertEquals("minecraft:server/status", requests.getFirst().path("method").asText());
        }
    }

    @Test
    void parameterizedCallsRetainNamedParams() throws Exception {
        List<JsonNode> requests = new ArrayList<>();
        try (var client = recordingClient(requests)) {
            client.session().save(true);
            assertEquals(1, requests.size());
            assertEquals("minecraft:server/save", requests.getFirst().path("method").asText());
            assertTrue(requests.getFirst().path("params").path("flush").asBoolean());
        }
    }

    private MinecraftManagementClient recordingClient(List<JsonNode> requests) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var client = new MinecraftManagementClient(mapper, URI.create("ws://localhost:25585"), "test");
        WebSocket socket = (WebSocket) Proxy.newProxyInstance(
                WebSocket.class.getClassLoader(), new Class<?>[]{WebSocket.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendText")) {
                        requests.add(mapper.readTree(args[0].toString()));
                    }
                    return CompletableFuture.completedFuture(proxy);
                });
        var field = MinecraftManagementClient.class.getDeclaredField("webSocket");
        field.setAccessible(true);
        field.set(client, socket);
        return client;
    }
}
