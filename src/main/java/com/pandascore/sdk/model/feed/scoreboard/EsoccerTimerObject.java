package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * eSoccer timer object (counts UP: 0-600 seconds for 2 halves).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EsoccerTimerObject {
    /** Match timer in seconds (0-300 = 1st half, 300-600 = 2nd half) */
    private Integer timer;
    /** Whether the timer is currently paused */
    private Boolean paused;
    /** ISO8601 timestamp when this snapshot was emitted */
    @JsonProperty("issued_at") private String issuedAt;
}
