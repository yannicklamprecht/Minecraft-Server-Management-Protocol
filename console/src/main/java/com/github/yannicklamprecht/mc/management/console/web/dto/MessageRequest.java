package com.github.yannicklamprecht.mc.management.console.web.dto;

/**
 * @param playerNameOrUuid when present, the message is sent privately to just this player instead
 *                          of broadcast to everyone.
 */
public record MessageRequest(String message, String playerNameOrUuid) {
}
