package com.example.msmp.generated.v3_1_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.Boolean;
import java.util.List;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record ServerState(@JsonProperty("players") List<Player> players,
        @JsonProperty("started") Boolean started, @JsonProperty("version") Version version) {
}
