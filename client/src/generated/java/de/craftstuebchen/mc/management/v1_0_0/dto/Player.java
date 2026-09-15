package de.craftstuebchen.mc.management.v1_0_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.String;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record Player(@JsonProperty("id") String id, @JsonProperty("name") String name) {
}
