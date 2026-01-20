package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Match-level score result.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchResult {
    /** Team ID */
    @JsonProperty("team_id") private Long teamId;
    /** Team score */
    private Integer score;
}
