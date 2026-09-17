package com.github.yannicklamprecht.mc.management.console.web;

import com.github.yannicklamprecht.mc.management.transport.JsonRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Translates failures from the MSMP session into clean JSON error responses instead of letting
 * them surface as an uncaught exception with a servlet-container stack trace. The Minecraft server
 * itself can reject a well-formed request at runtime (e.g. kicking a player whose connection is
 * already being torn down responds with a generic "Internal error"), so callers of {@code /api/**}
 * need a structured, low-drama way to see what happened.
 */
@RestControllerAdvice
public class ManagementApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementApiExceptionHandler.class);

    @ExceptionHandler(JsonRpcException.class)
    public ResponseEntity<Map<String, Object>> handleJsonRpcException(JsonRpcException ex) {
        LOGGER.warn("MSMP server rejected request: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.error().message());
        body.put("code", ex.error().code());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleNotConnected(IllegalStateException ex) {
        LOGGER.warn("MSMP request failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
    }

    /** Thrown by {@code MinecraftManagementServerRegistry} for an unknown {@code {serverId}}. */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleUnknownServer(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
