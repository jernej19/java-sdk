package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Top-level fixture message from the feed.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureMessage {
    private String type;
    private String at;
    private FixtureAction action;
    @JsonProperty("event_type") private String eventType;
    @JsonProperty("event_id") private Long eventId;
    @JsonProperty("videogame_slug") private String videogameSlug;
    @JsonProperty("tournament_tier") private String tournamentTier;
    @JsonProperty("match_id") private Long matchId;
    @JsonProperty("game_position") private Integer gamePosition;
    private FixtureMatch match;
    private FixtureSerie serie;
}
