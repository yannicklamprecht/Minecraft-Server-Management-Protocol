package com.example.msmp.generated.v3_0_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record KickPlayer(@JsonProperty("message") Message message,
        @JsonProperty("player") Player player) {
}
