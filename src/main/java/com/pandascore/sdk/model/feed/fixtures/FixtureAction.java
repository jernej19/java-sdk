package com.pandascore.sdk.model.feed.fixtures;

/**
 * Possible actions that can occur on a fixture message.
 */
public enum FixtureAction {
    created, booked, unbooked, reviewed, updated,
    rescheduled, postponed, started, finished,
    settled, coverage_changed, canceled, deleted,
    /** @deprecated Legacy action still sent for backward compatibility */
    @Deprecated
    live_available
}
