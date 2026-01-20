package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * eBasketball timer object (counts DOWN: 300→0 seconds each quarter).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EbasketballTimerObject {
    /** Quarter timer in seconds (counts DOWN 300 → 0 each quarter) */
    private Integer timer;
    /** Whether the timer is currently paused */
    private Boolean paused;
    /** ISO8601 timestamp when this snapshot was emitted */
    @JsonProperty("issued_at") private String issuedAt;
}
