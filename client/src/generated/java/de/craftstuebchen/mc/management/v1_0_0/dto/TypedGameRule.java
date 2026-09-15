package de.craftstuebchen.mc.management.v1_0_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.String;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record TypedGameRule(@JsonProperty("type") String type, @JsonProperty("key") String key,
        @JsonProperty("value") String value) {
}
