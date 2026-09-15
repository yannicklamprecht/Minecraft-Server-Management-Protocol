package de.craftstuebchen.mc.management.v3_0_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.Object;
import java.lang.String;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record UntypedGameRule(@JsonProperty("key") String key,
        @JsonProperty("value") Object value) {
}
