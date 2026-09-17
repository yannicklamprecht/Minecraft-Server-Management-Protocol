package com.github.yannicklamprecht.mc.management.console.web;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.console.config.ConsoleUiProperties;
import com.github.yannicklamprecht.mc.management.spring.MinecraftManagementClientLifecycle;
import com.github.yannicklamprecht.mc.management.spring.MinecraftManagementServerRegistry;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRecord;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagementApiController.class)
class ManagementApiActivityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MinecraftManagementServerRegistry registry;

    @MockitoBean
    private MinecraftManagementClientLifecycle lifecycle;

    @MockitoBean
    private MinecraftManagementSession session;

    @MockitoBean
    private ActivityRepository activityRepository;

    @MockitoBean
    private ConsoleUiProperties uiProperties;

    @Test
    void returnsTheActivityRecordedForThatServerOnly() throws Exception {
        when(activityRepository.findRecentForServer(eq("survival"), eq(200))).thenReturn(List.of(
                new ActivityRecord("survival", "PlayerJoined", null, Instant.parse("2026-09-17T10:00:00Z"))));

        mockMvc.perform(get("/api/servers/survival/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serverId").value("survival"))
                .andExpect(jsonPath("$[0].type").value("PlayerJoined"));
    }

    @Test
    void honorsAnExplicitLimit() throws Exception {
        when(activityRepository.findRecentForServer(eq("survival"), eq(5))).thenReturn(List.of());

        mockMvc.perform(get("/api/servers/survival/activity").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void sendingASystemMessageIsPersistedAsActivity() throws Exception {
        // MSMP has no notification for an outgoing system message (unlike saves, kicks, or
        // operator/allowlist/ban changes), so the controller must record it itself - otherwise it
        // would never appear in the activity history after a reload.
        when(registry.session(anyString())).thenReturn(session);
        when(session.sendSystemMessage(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        MvcResult asyncResult = mockMvc.perform(post("/api/servers/survival/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello everyone\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult)).andExpect(status().isOk());

        verify(activityRepository).record(argThat(record ->
                record.serverId().equals("survival")
                        && record.type().equals("MessageSent")
                        && record.payload().equals("hello everyone")));
    }
}
