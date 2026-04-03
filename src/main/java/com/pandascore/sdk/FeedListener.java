package com.pandascore.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandascore.sdk.model.feed.fixtures.FixtureMessage;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;

/**
 * Typed callback interface for PandaScore feed messages.
 * <p>
 * Override only the methods you care about — all defaults are no-ops.
 * Pass an implementation to {@link com.pandascore.sdk.rmq.RabbitMQFeed#connect(FeedListener)}
 * for automatic JSON deserialization and dispatch.
 *
 * <pre>{@code
 * feed.connect(new FeedListener() {
 *     @Override
 *     public void onMarkets(MarketsMessage msg) {
 *         System.out.println("Markets: " + msg.getMatchId());
 *     }
 * });
 * }</pre>
 */
public interface FeedListener {

    /**
     * Called for messages with {@code type = "markets"}.
     *
     * @param message deserialized markets message
     */
    default void onMarkets(MarketsMessage message) {}

    /**
     * Called for messages with {@code type = "fixture"}.
     *
     * @param message deserialized fixture message
     */
    default void onFixture(FixtureMessage message) {}

    /**
     * Called for messages with {@code type = "scoreboard"}.
     * <p>
     * Scoreboard messages are game-specific (CS, Dota2, LoL, Valorant, etc.)
     * and are differentiated by the {@code scoreboard_type} JSON field.
     * The raw JSON is provided so you can deserialize to the specific model:
     * <pre>{@code
     * switch (scoreboardType) {
     *     case "cs"   -> mapper.treeToValue(raw, ScoreboardCs.class);
     *     case "lol"  -> mapper.treeToValue(raw, ScoreboardLol.class);
     *     // ...
     * }
     * }</pre>
     *
     * @param raw            the raw JSON node
     * @param scoreboardType the scoreboard_type field value (e.g. "cs", "lol", "dota2")
     */
    default void onScoreboard(JsonNode raw, String scoreboardType) {}

    /**
     * Called for messages with an unrecognized or missing {@code type} field.
     *
     * @param raw the raw JSON node
     */
    default void onUnknown(JsonNode raw) {}
}
