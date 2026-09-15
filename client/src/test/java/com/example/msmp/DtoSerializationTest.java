package com.example.msmp;


import com.fasterxml.jackson.databind.ObjectMapper;
import de.craftstuebchen.mc.management.v3_1_0.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoSerializationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testPlayerSerialization() throws Exception {
        Player player = new Player("12345-6789", "Steve");
        String json = mapper.writeValueAsString(player);
        assertTrue(json.contains("\"name\":\"Steve\""));
        assertTrue(json.contains("\"id\":\"12345-6789\""));

        Player deserialized = mapper.readValue(json, Player.class);
        assertEquals("Steve", deserialized.name());
        assertEquals("12345-6789", deserialized.id());
    }

    @Test
    void testDifficultyEnum() throws Exception {
        String jsonPeaceful = mapper.writeValueAsString(Difficulty.PEACEFUL);
        assertEquals("\"peaceful\"", jsonPeaceful);

        Difficulty difficulty = mapper.readValue("\"hard\"", Difficulty.class);
        assertEquals(Difficulty.HARD, difficulty);
        assertEquals("hard", difficulty.getValue());
    }

    @Test
    void testServerStateSerialization() throws Exception {
        Version version = new Version("1.21.4", 768L);
        Player player = new Player("uuid-1", "Alex");
        ServerState state = new ServerState(List.of(player), true, version);

        String json = mapper.writeValueAsString(state);
        assertTrue(json.contains("\"started\":true"));
        assertTrue(json.contains("\"name\":\"1.21.4\""));

        ServerState deserialized = mapper.readValue(json, ServerState.class);
        assertTrue(deserialized.started());
        assertEquals(1, deserialized.players().size());
        assertEquals("Alex", deserialized.players().get(0).name());
        assertEquals(768L, deserialized.version().protocol());
    }

    @Test
    void testOperatorSerialization() throws Exception {
        Player player = new Player("uuid-op", "Admin");
        Operator operator = new Operator(true, 4L, player);

        String json = mapper.writeValueAsString(operator);
        assertTrue(json.contains("\"bypassesPlayerLimit\":true"));
        assertTrue(json.contains("\"permissionLevel\":4"));

        Operator deserialized = mapper.readValue(json, Operator.class);
        assertTrue(deserialized.bypassesPlayerLimit());
        assertEquals(4L, deserialized.permissionLevel());
        assertEquals("Admin", deserialized.player().name());
    }

    @Test
    void testIgnoreUnknownProperties() throws Exception {
        String extraJson = "{\"id\":\"u1\",\"name\":\"Steve\",\"extraField\":\"unknown_value\"}";
        Player player = mapper.readValue(extraJson, Player.class);
        assertEquals("Steve", player.name());
        assertEquals("u1", player.id());
    }
}
