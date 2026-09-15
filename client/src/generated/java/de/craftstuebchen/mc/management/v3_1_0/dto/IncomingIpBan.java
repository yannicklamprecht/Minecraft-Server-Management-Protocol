package de.craftstuebchen.mc.management.v3_1_0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.String;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record IncomingIpBan(@JsonProperty("expires") String expires, @JsonProperty("ip") String ip,
        @JsonProperty("player") Player player, @JsonProperty("reason") String reason,
        @JsonProperty("source") String source) {
}
