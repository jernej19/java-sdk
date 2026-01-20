package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Round score by team.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoundScore {
    /** Team ID */
    @JsonProperty("team_id") private Long teamId;
    /** Round score for this team */
    private Integer score;
}
