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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers {@code console.ui.operator-notifications}: kicking/banning a player, or making/unmaking
 * one an operator, should privately message every other online operator - excluding whoever the
 * action was performed on - governed by the configured {@link OperatorNotificationMode}.
 */
@WebMvcTest(ManagementApiController.class)
class ManagementApiOperatorNotificationTests {

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
        when(session.addOperator(anyString(), anyLong(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(null));
        when(session.removeOperator(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(session.kickPlayer(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(session.sendPrivateMessage(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(true));
    }

    private static MinecraftManagementSession.OperatorView operator(String id, String name) {
        return new MinecraftManagementSession.OperatorView(false, 4, new MinecraftManagementSession.PlayerView(id, name));
    }

    @Test
    void alwaysModeNotifiesOtherOperatorsButNotTheTarget() throws Exception {
        when(uiProperties.getOperatorNotifications()).thenReturn(OperatorNotificationMode.ALWAYS);
        when(session.getOperators()).thenReturn(CompletableFuture.completedFuture(
                List.of(operator("id-alice", "Alice"), operator("id-bob", "Bob"))));

        MvcResult async = mockMvc.perform(post("/api/servers/survival/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"Alice\",\"permissionLevel\":4,\"bypassesPlayerLimit\":false}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(async)).andExpect(status().isOk());

        verify(session).sendPrivateMessage(eq("id-bob"), anyString());
        verify(session, never()).sendPrivateMessage(eq("id-alice"), anyString());
    }

    @Test
    void removingAnOperatorNotifiesTheRestButNotTheRemovedOperator() throws Exception {
        when(uiProperties.getOperatorNotifications()).thenReturn(OperatorNotificationMode.ALWAYS);
        when(session.getOperators()).thenReturn(CompletableFuture.completedFuture(
                List.of(operator("id-alice", "Alice"), operator("id-bob", "Bob"))));

        MvcResult async = mockMvc.perform(delete("/api/servers/survival/operators/Alice"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(async)).andExpect(status().isOk());

        verify(session).sendPrivateMessage(eq("id-bob"), anyString());
        verify(session, never()).sendPrivateMessage(eq("id-alice"), anyString());
    }

    @Test
    void neverModeSendsNoMessagesEvenWhenTheRequestAsksFor() throws Exception {
        when(uiProperties.getOperatorNotifications()).thenReturn(OperatorNotificationMode.NEVER);

        MvcResult async = mockMvc.perform(post("/api/servers/survival/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"Alice\",\"permissionLevel\":4,\"bypassesPlayerLimit\":false,\"notifyOperators\":true}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(async)).andExpect(status().isOk());

        verify(session, never()).getOperators();
        verify(session, never()).sendPrivateMessage(anyString(), anyString());
    }

    @Test
    void defaultFalseModeOnlyNotifiesWhenTheRequestOptsIn() throws Exception {
        when(uiProperties.getOperatorNotifications()).thenReturn(OperatorNotificationMode.DEFAULT_FALSE);
        when(session.getOperators()).thenReturn(CompletableFuture.completedFuture(List.of(operator("id-bob", "Bob"))));

        // notifyOperators omitted - falls back to this mode's own default (unchecked -> false).
        MvcResult withoutFlag = mockMvc.perform(post("/api/servers/survival/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"Alice\",\"permissionLevel\":4,\"bypassesPlayerLimit\":false}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(withoutFlag)).andExpect(status().isOk());
        verify(session, never()).sendPrivateMessage(anyString(), anyString());

        // Explicitly opted in this time.
        MvcResult withFlag = mockMvc.perform(post("/api/servers/survival/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"Alice\",\"permissionLevel\":4,\"bypassesPlayerLimit\":false,\"notifyOperators\":true}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(withFlag)).andExpect(status().isOk());
        verify(session).sendPrivateMessage(eq("id-bob"), anyString());
    }

    @Test
    void kickAlsoNotifiesOtherOperators() throws Exception {
        when(uiProperties.getOperatorNotifications()).thenReturn(OperatorNotificationMode.ALWAYS);
        when(session.getOperators()).thenReturn(CompletableFuture.completedFuture(List.of(operator("id-bob", "Bob"))));

        MvcResult async = mockMvc.perform(post("/api/servers/survival/players/kick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"griefer123\",\"reason\":\"griefing\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(async)).andExpect(status().isOk());

        verify(session).sendPrivateMessage(eq("id-bob"), eq("griefer123 was kicked (griefing)."));
    }

    @Test
    void kickNotificationUsesTheSuppliedDisplayNameInsteadOfTheRawUuid() throws Exception {
        when(uiProperties.getOperatorNotifications()).thenReturn(OperatorNotificationMode.ALWAYS);
        when(session.getOperators()).thenReturn(CompletableFuture.completedFuture(List.of(operator("id-bob", "Bob"))));

        MvcResult async = mockMvc.perform(post("/api/servers/survival/players/kick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"11111111-1111-1111-1111-111111111111\",\"reason\":\"griefing\",\"playerDisplayName\":\"Griefer\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(async)).andExpect(status().isOk());

        verify(session).sendPrivateMessage(eq("id-bob"), eq("Griefer was kicked (griefing)."));
    }

    @Test
    void removingAnOperatorNotificationUsesTheSuppliedDisplayName() throws Exception {
        when(uiProperties.getOperatorNotifications()).thenReturn(OperatorNotificationMode.ALWAYS);
        when(session.getOperators()).thenReturn(CompletableFuture.completedFuture(
                List.of(operator("11111111-1111-1111-1111-111111111111", "Alice"), operator("id-bob", "Bob"))));

        MvcResult async = mockMvc.perform(delete("/api/servers/survival/operators/11111111-1111-1111-1111-111111111111")
                        .param("playerDisplayName", "Alice"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(async)).andExpect(status().isOk());

        verify(session).sendPrivateMessage(eq("id-bob"), eq("Alice is no longer an operator."));
        verify(session, never()).sendPrivateMessage(eq("11111111-1111-1111-1111-111111111111"), anyString());
    }
}
