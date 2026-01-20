package com.pandascore.sdk.model.feed.markets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Represents a game wrapper returned by the markets recovery endpoint.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketsRecoveryGame {
    /** Identifier of the game. */
    private Long id;
    /** Position of the game within the match. */
    private Integer position;
    /** Markets associated with this game. */
    private List<MarketsMessageMarket> markets;
}

