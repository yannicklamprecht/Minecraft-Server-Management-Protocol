package de.craftstuebchen.mc.management.v2_0_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.Long;
import java.lang.String;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record Version(@JsonProperty("name") String name, @JsonProperty("protocol") Long protocol) {
}
