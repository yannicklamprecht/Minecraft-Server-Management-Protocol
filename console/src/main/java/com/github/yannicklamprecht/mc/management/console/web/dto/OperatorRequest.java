package com.github.yannicklamprecht.mc.management.console.web.dto;

/**
 * @param notifyOperators   whether to also privately message the other online operators about this
 *                           change; ignored (server-side mode wins) unless
 *                           {@code console.ui.operator-notifications} is one of the two checkbox
 *                           modes - see {@code OperatorNotificationMode}.
 * @param playerDisplayName the player's human-readable name, when the caller already has it (e.g.
 *                           the dashboard's player list, where {@code playerNameOrUuid} is a UUID) -
 *                           used in that operator notification instead of the raw UUID. Falls back
 *                           to {@code playerNameOrUuid} itself when omitted.
 */
public record OperatorRequest(String playerNameOrUuid, long permissionLevel, boolean bypassesPlayerLimit,
                               Boolean notifyOperators, String playerDisplayName) {
}
