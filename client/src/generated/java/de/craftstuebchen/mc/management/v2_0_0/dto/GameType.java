package de.craftstuebchen.mc.management.v2_0_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.lang.String;

public enum GameType {
    @JsonProperty("survival")
    SURVIVAL("survival"),

    @JsonProperty("creative")
    CREATIVE("creative"),

    @JsonProperty("adventure")
    ADVENTURE("adventure"),

    @JsonProperty("spectator")
    SPECTATOR("spectator");

    private final String value;

    GameType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    public String value() {
        return this.value;
    }
}
