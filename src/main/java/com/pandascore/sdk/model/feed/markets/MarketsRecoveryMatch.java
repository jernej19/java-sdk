package com.pandascore.sdk.model.feed.markets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Top-level element returned by the markets recovery endpoint.
 * Each element represents a match with its markets and games.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketsRecoveryMatch {
    /** Match identifier. */
    private Long id;
    /** Markets directly associated with the match. */
    private List<MarketsMessageMarket> markets;
    /** Games belonging to this match. */
    private List<MarketsRecoveryGame> games;
}

