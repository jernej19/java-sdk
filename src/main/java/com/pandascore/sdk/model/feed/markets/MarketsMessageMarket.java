package com.pandascore.sdk.model.feed.markets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Market details contained within a markets message.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketsMessageMarket {
    /** Identifier of the market. */
    private String id;
    /** Line value associated with the market. */
    private String line;
    /** Display name of the market. */
    private String name;
    /** Current status of the market. */
    private String status;
    /** Template used to build this market. */
    private String template;
    /** Identifier of the participant the market refers to. */
    @JsonProperty("participant_id") private Long participantId;
    /** Type of participant (team, player…). */
    @JsonProperty("participant_type") private String participantType;
    /** Overround applied to the market probabilities. */
    private Double overround;
    /** Margin taken by the bookmaker. */
    private Double margin;
    /** Whether the market has been reviewed. */
    private Boolean reviewed;
    /** Timestamp when the market will auto-deactivate. */
    @JsonProperty("auto_deactivated_at") private String autoDeactivatedAt;
    /** Index of the drake objective, if applicable. */
    @JsonProperty("drake_index") private Integer drakeIndex;
    /** Index of the Nashor objective. */
    @JsonProperty("nashor_index") private Integer nashorIndex;
    /** Index of the tower objective. */
    @JsonProperty("tower_index") private Integer towerIndex;
    /** Index of the rift herald objective. */
    @JsonProperty("rift_herald_index") private Integer riftHeraldIndex;
    /** Identifier of the champion used. */
    @JsonProperty("champion_id") private Long championId;
    /** Timer value for time based markets. */
    private Integer timer;
    /** Start of the time window for the market. */
    @JsonProperty("time_window_starts_at") private Long timeWindowStartsAt;
    /** End of the time window for the market. */
    @JsonProperty("time_window_ends_at") private Long timeWindowEndsAt;
    /** Handicap value for the away side. */
    @JsonProperty("handicap_away") private Double handicapAway;
    /** Handicap value for the home side. */
    @JsonProperty("handicap_home") private Double handicapHome;
    /** Grouping key for dynamic lines. */
    @JsonProperty("dynamic_line_grouping_key") private String dynamicLineGroupingKey;
    /** Side of the participant, when relevant. */
    @JsonProperty("participant_side") private String participantSide;
    /** Identifier of the away player for the market. */
    @JsonProperty("player_away_id") private Long playerAwayId;
    /** Identifier of the home player for the market. */
    @JsonProperty("player_home_id") private Long playerHomeId;
    /** Identifier of the team. */
    @JsonProperty("team_id") private Long teamId;
    /** Identifier of the player this market concerns. */
    @JsonProperty("player_id") private Long playerId;
    /** Number of player kills relevant to the market. */
    @JsonProperty("player_kills") private Integer playerKills;
    /** Parent selection identifiers for prebuilt markets. */
    @JsonProperty("prebuilt_parent_selection_ids") private List<String> prebuiltParentSelectionIds;
    /** Short description providing context. */
    private String story;
    /** Index of the quarter in eBasketball/eSoccer. */
    @JsonProperty("quarter_index") private Integer quarterIndex;
    /** Index of the half in eSoccer. */
    @JsonProperty("half_index") private Integer halfIndex;
    /** Index of the goal in eSoccer. */
    @JsonProperty("goal_index") private Integer goalIndex;
    /** Index of the round in games such as CS:GO. */
    @JsonProperty("round_index") private Integer roundIndex;
    /** Selections offered within this market. */
    private List<MarketsMessageSelection> selections;
}
