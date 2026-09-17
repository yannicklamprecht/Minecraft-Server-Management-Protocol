package com.github.yannicklamprecht.mc.management.console.web.dto;

/** Used for allowlist and operator removal, which accept a player name or UUID. */
public record PlayerNameRequest(String playerNameOrUuid) {
}
