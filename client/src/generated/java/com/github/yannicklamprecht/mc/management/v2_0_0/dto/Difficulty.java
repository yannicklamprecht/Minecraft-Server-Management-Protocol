package com.github.yannicklamprecht.mc.management.v2_0_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.lang.String;

public enum Difficulty {
    @JsonProperty("peaceful")
    PEACEFUL("peaceful"),

    @JsonProperty("easy")
    EASY("easy"),

    @JsonProperty("normal")
    NORMAL("normal"),

    @JsonProperty("hard")
    HARD("hard");

    private final String value;

    Difficulty(String value) {
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
