package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * eHockey timer object (counts DOWN: 300→0 seconds each period).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EhockeyTimerObject {
    /** Period timer in seconds (counts DOWN 300 → 0 each period) */
    private Integer timer;
    /** Whether the timer is currently paused */
    private Boolean paused;
    /** ISO8601 timestamp when this snapshot was emitted */
    @JsonProperty("issued_at") private String issuedAt;
}
