package com.example.msmp.generated.v2_0_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.Boolean;
import java.lang.Long;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record Operator(@JsonProperty("bypassesPlayerLimit") Boolean bypassesPlayerLimit,
        @JsonProperty("permissionLevel") Long permissionLevel,
        @JsonProperty("player") Player player) {
}
