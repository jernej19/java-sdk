package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Detailed round-by-round data (e.g., CS:GO).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameRoundTeam {
    /** Round outcome (e.g., "eliminated", "exploded", "defused") */
    private String outcome;
    /** Round number */
    private Integer round;
    /** Team ID */
    @JsonProperty("team_id") private Long teamId;
    /** Whether team was terrorist side */
    private Boolean terrorist;
    /** Whether team won the round */
    private Boolean winner;
}
