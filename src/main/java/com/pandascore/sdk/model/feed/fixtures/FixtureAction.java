package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Possible actions that can occur on a fixture message.
 *
 * <p>Known actions are represented as enum constants. Unknown actions received
 * from the feed are mapped to {@link #UNKNOWN} so that deserialization never
 * fails when new action values are introduced server-side.</p>
 *
 * <p>Use {@link #getValue()} to obtain the original string value from the feed,
 * which is especially useful when the action is {@code UNKNOWN}.</p>
 */
public enum FixtureAction {
    created("created"),
    booked("booked"),
    unbooked("unbooked"),
    reviewed("reviewed"),
    updated("updated"),
    rescheduled("rescheduled"),
    postponed("postponed"),
    started("started"),
    finished("finished"),
    settled("settled"),
    coverage_changed("coverage_changed"),
    canceled("canceled"),
    deleted("deleted"),
    opponents_swapped("opponents_swapped"),
    opponents_updated("opponents_updated"),
    /** @deprecated Legacy action still sent for backward compatibility */
    @Deprecated
    live_available("live_available"),
    /** @deprecated Legacy action still sent for backward compatibility */
    @Deprecated
    live_not_available("live_not_available"),
    /**
     * Represents an action value not yet known to this version of the SDK.
     * Call {@link #getValue()} to retrieve the original string.
     */
    UNKNOWN(null);

    private final String value;

    FixtureAction(String value) {
        this.value = value;
    }

    /**
     * Returns the original action string from the feed.
     * For {@link #UNKNOWN} instances created via {@link #fromValue(String)},
     * this returns the raw string that was received.
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Deserializes a JSON action string into a {@code FixtureAction}.
     * Returns a matching enum constant if one exists, otherwise returns {@link #UNKNOWN}.
     *
     * @param value the action string from the feed
     * @return the corresponding enum constant, or {@code UNKNOWN} for unrecognized values
     */
    @JsonCreator
    public static FixtureAction fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (FixtureAction action : values()) {
            if (action.value != null && action.value.equals(value)) {
                return action;
            }
        }
        return UNKNOWN;
    }
}
