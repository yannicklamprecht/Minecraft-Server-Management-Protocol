package com.github.yannicklamprecht.mc.management.v2_0_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.Object;
import java.lang.String;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record TypedGameRule(@JsonProperty("type") String type, @JsonProperty("key") String key,
        @JsonProperty("value") Object value) {
}
