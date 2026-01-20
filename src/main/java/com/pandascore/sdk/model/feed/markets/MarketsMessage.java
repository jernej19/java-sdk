package com.pandascore.sdk.model.feed.markets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Top-level markets message from the feed.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketsMessage {
    /** Type of message received from the feed. */
    private String type;
    /** Timestamp of the message. */
    private String at;
    /** Action that occurred on the market. */
    private MarketAction action;
    /** Event type such as "match" or "game". */
    @JsonProperty("event_type") private String eventType;
    /** Identifier of the related event. */
    @JsonProperty("event_id") private Long eventId;
    /** Slug of the videogame associated with the event. */
    @JsonProperty("videogame_slug") private String videogameSlug;
    /** Match identifier if applicable. */
    @JsonProperty("match_id") private Long matchId;
    /** Tier of the tournament. */
    @JsonProperty("tournament_tier") private String tournamentTier;
    /** Position of the game inside the match. */
    @JsonProperty("game_position") private Integer gamePosition;
    /** Markets included in this message. */
    private List<MarketsMessageMarket> markets;
}
