package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a group of betting markets for a fixture.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BettingGroup {
    private Long id;
    private String name;
}
