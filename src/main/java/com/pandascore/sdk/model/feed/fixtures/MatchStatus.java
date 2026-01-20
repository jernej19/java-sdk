package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of a match in the betting lifecycle.
 */
public enum MatchStatus {
    NOT_BOOKED("not_booked"),
    PENDING("pending"),
    PRE_MATCH("pre_match"),
    LIVE("live"),
    POSTPONED("postponed"),
    FINISHED("finished"),
    SETTLED("settled"),
    CANCELED("canceled");

    private final String value;

    MatchStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
