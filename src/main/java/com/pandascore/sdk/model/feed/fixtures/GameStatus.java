package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of an individual game/map within a match.
 */
public enum GameStatus {
    NOT_STARTED("not_started"),
    RUNNING("running"),
    FINISHED("finished"),
    NOT_PLAYED("not_played");

    private final String value;

    GameStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
