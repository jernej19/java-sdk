package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Opponent entry within a fixture.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureOpponent {
    private String type;
    @JsonProperty("playing_as") private String playingAs;
    private FixtureTeam opponent;
}
