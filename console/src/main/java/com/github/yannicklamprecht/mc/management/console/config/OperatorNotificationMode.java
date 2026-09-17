package com.github.yannicklamprecht.mc.management.console.config;

/**
 * Controls whether kicking/banning a player, or making/unmaking one an operator, also privately
 * messages every other currently-online operator about it - and whether the dashboard's "Notify
 * operators" checkbox (which lets an admin override that per action) is shown at all.
 */
public enum OperatorNotificationMode {

    /** Never notify operators, and never show the checkbox - the choice isn't offered. */
    NEVER,

    /** Always notify operators; the checkbox isn't shown since there's nothing to choose. */
    ALWAYS,

    /** Show the checkbox, checked by default; notifies unless the admin unchecks it. */
    DEFAULT_TRUE,

    /** Show the checkbox, unchecked by default; only notifies if the admin checks it. */
    DEFAULT_FALSE;

    /** Whether the frontend should render the "Notify operators" checkbox at all. */
    public boolean showsCheckbox() {
        return this == DEFAULT_TRUE || this == DEFAULT_FALSE;
    }

    /** The checkbox's initial checked state, for the modes that show one. */
    public boolean defaultChecked() {
        return this == DEFAULT_TRUE;
    }

    /**
     * Whether to actually send the notifications for one specific action, given what the request
     * asked for. {@code NEVER}/{@code ALWAYS} are authoritative regardless of {@code requested} -
     * the server-side mode always wins, a client can't force notifications on or off against it.
     * For the two checkbox modes, a missing/{@code null} {@code requested} falls back to this
     * mode's own default rather than being treated as "off".
     */
    public boolean shouldNotify(Boolean requested) {
        return switch (this) {
            case NEVER -> false;
            case ALWAYS -> true;
            case DEFAULT_TRUE, DEFAULT_FALSE -> requested != null ? requested : defaultChecked();
        };
    }
}
