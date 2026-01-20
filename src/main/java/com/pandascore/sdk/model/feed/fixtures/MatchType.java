package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type of match format.
 */
public enum MatchType {
    BEST_OF("best_of"),
    OW_BEST_OF("ow_best_of"),
    FIRST_TO("first_to"),
    RED_BULL_HOME_GROUND("red_bull_home_ground"),
    CUSTOM("custom");

    private final String value;

    MatchType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
