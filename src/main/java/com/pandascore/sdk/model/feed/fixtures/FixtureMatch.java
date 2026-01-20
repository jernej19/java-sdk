package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Details about a match within a fixture message.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureMatch {
    private Long id;
    private String name;
    @JsonProperty("scheduled_at") private String scheduledAt;
    @JsonProperty("begin_at") private String beginAt;
    @JsonProperty("detailed_stats") private boolean detailedStats;
    @JsonProperty("betting_metadata") private FixtureMatchBettingMetadata bettingMetadata;
    @JsonProperty("league_id") private Long leagueId;
    private String status;
    private List<FixtureOpponent> opponents;
    private FixtureWinner winner;
    private FixtureSerie serie;
}
