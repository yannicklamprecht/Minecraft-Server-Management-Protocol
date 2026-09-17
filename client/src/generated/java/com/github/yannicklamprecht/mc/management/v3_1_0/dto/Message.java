package com.github.yannicklamprecht.mc.management.v3_1_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.String;
import java.util.List;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record Message(@JsonProperty("literal") String literal,
        @JsonProperty("translatable") String translatable,
        @JsonProperty("translatableParams") List<String> translatableParams) {
}
