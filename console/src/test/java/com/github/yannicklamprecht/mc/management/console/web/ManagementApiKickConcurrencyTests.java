package com.github.yannicklamprecht.mc.management.console.web;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.console.config.ConsoleUiProperties;
import com.github.yannicklamprecht.mc.management.console.config.OperatorNotificationMode;
import com.github.yannicklamprecht.mc.management.spring.MinecraftManagementClientLifecycle;
import com.github.yannicklamprecht.mc.management.spring.MinecraftManagementServerRegistry;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kicking a player is not idempotent on the Minecraft server: a second {@code players/kick} for a
 * connection already being torn down throws {@code IllegalStateException: Already retired}
 * server-side. {@link ManagementApiController} must reject a second concurrent kick for the same
 * player outright rather than forwarding it to the server - this protects against multiple
 * browser tabs, a page reload while a kick is in flight, or any other caller, not just a single
 * page's own re-render/double-click guard.
 */
@WebMvcTest(ManagementApiController.class)
class ManagementApiKickConcurrencyTests {

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

    @BeforeEach
    void wireUpFakeServer() {
        when(registry.session(anyString())).thenReturn(session);
        // Not exercising operator-notification behavior here - NEVER keeps it out of the way so
        // this test's mocks stay scoped to what it's actually testing (the in-flight kick guard).
        when(uiProperties.getOperatorNotifications()).thenReturn(OperatorNotificationMode.NEVER);
    }

    @Test
    void secondConcurrentKickForSamePlayerIsRejectedWithoutReachingTheServer() throws Exception {
        CompletableFuture<Void> firstKick = new CompletableFuture<>();
        when(session.kickPlayer(anyString(), anyString())).thenReturn(firstKick);

        MvcResult firstAsync = mockMvc.perform(post("/api/servers/test-server/players/kick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"ysl3000\",\"reason\":\"first\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // A second kick for the SAME player on the SAME server while the first is still pending
        // must be rejected immediately - it must never reach session.kickPlayer a second time.
        // The rejection is itself a completed CompletableFuture, but Spring MVC still routes any
        // CompletableFuture return value through async processing, so it needs the same
        // two-phase dispatch.
        MvcResult secondAsync = mockMvc.perform(post("/api/servers/test-server/players/kick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"ysl3000\",\"reason\":\"second\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(secondAsync))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        verify(session, times(1)).kickPlayer(anyString(), anyString());

        firstKick.complete(null);
        mockMvc.perform(asyncDispatch(firstAsync)).andExpect(status().isOk());

        // Once the first kick has completed, the player id is free again for a new attempt.
        when(session.kickPlayer(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        MvcResult thirdAsync = mockMvc.perform(post("/api/servers/test-server/players/kick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"ysl3000\",\"reason\":\"third\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(thirdAsync)).andExpect(status().isOk());

        verify(session, times(2)).kickPlayer(anyString(), anyString());
    }

    @Test
    void sameKickIsIndependentAcrossDifferentServers() throws Exception {
        CompletableFuture<Void> firstServerKick = new CompletableFuture<>();
        when(session.kickPlayer(anyString(), anyString())).thenReturn(firstServerKick);

        // Same player name, but a different {serverId} in the path - must not collide with the
        // in-flight guard for "server-a".
        mockMvc.perform(post("/api/servers/server-a/players/kick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"ysl3000\",\"reason\":\"test\"}"))
                .andExpect(request().asyncStarted());

        when(session.kickPlayer(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        MvcResult otherServerAsync = mockMvc.perform(post("/api/servers/server-b/players/kick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"ysl3000\",\"reason\":\"test\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(otherServerAsync)).andExpect(status().isOk());

        verify(session, times(2)).kickPlayer(anyString(), anyString());
    }
}
