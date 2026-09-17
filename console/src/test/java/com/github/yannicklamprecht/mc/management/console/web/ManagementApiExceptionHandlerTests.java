package com.github.yannicklamprecht.mc.management.console.web;

import com.github.yannicklamprecht.mc.management.api.MinecraftManagementSession;
import com.github.yannicklamprecht.mc.management.console.config.ConsoleUiProperties;
import com.github.yannicklamprecht.mc.management.console.config.OperatorNotificationMode;
import com.github.yannicklamprecht.mc.management.spring.MinecraftManagementClientLifecycle;
import com.github.yannicklamprecht.mc.management.spring.MinecraftManagementServerRegistry;
import com.github.yannicklamprecht.mc.management.spring.activity.ActivityRepository;
import com.github.yannicklamprecht.mc.management.transport.JsonRpcError;
import com.github.yannicklamprecht.mc.management.transport.JsonRpcException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Minecraft server can reject a well-formed request at runtime (e.g. kicking a player whose
 * connection is already being torn down responds with a generic "Internal error"). Without
 * {@link ManagementApiExceptionHandler}, that failure propagated as an uncaught exception all the
 * way to the servlet container instead of a clean API error.
 */
@WebMvcTest(ManagementApiController.class)
class ManagementApiExceptionHandlerTests {

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
        // this test's mocks stay scoped to what it's actually testing (error translation).
        when(uiProperties.getOperatorNotifications()).thenReturn(OperatorNotificationMode.NEVER);
    }

    @Test
    void jsonRpcErrorFromTheServerBecomesACleanJsonErrorResponse() throws Exception {
        when(session.kickPlayer(anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new JsonRpcException(new JsonRpcError(-32603, "Internal error", null))));

        MvcResult asyncResult = mockMvc.perform(post("/api/servers/test-server/players/kick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerNameOrUuid\":\"ysl3000\",\"reason\":\"test\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Internal error"))
                .andExpect(jsonPath("$.code").value(-32603));
    }
}
