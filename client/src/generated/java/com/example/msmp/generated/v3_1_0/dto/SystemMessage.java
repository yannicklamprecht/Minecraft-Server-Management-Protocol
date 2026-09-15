package com.example.msmp.generated.v3_1_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.Boolean;
import java.util.List;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record SystemMessage(@JsonProperty("message") Message message,
        @JsonProperty("overlay") Boolean overlay,
        @JsonProperty("receivingPlayers") List<Player> receivingPlayers) {
}
