package com.pandascore.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandascore.sdk.model.feed.fixtures.FixtureMessage;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TypedFeedAdapter — message routing and deserialization.
 */
class TypedFeedAdapterTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ============================================================
    //  Markets routing
    // ============================================================

    @Test
    @DisplayName("Markets message is routed to onMarkets with deserialized MarketsMessage")
    void marketsMessage_routedToOnMarkets() throws Exception {
        String json = """
            {
              "type": "markets",
              "match_id": 42,
              "action": "odds_changed",
              "markets": []
            }
            """;
        JsonNode node = mapper.readTree(json);

        AtomicReference<MarketsMessage> captured = new AtomicReference<>();
        TypedFeedAdapter adapter = new TypedFeedAdapter(new FeedListener() {
            @Override
            public void onMarkets(MarketsMessage message) {
                captured.set(message);
            }
        });

        adapter.accept(node);

        assertNotNull(captured.get());
        assertEquals(42L, captured.get().getMatchId());
    }

    // ============================================================
    //  Fixture routing
    // ============================================================

    @Test
    @DisplayName("Fixture message is routed to onFixture with deserialized FixtureMessage")
    void fixtureMessage_routedToOnFixture() throws Exception {
        String json = """
            {
              "type": "fixture",
              "match_id": 99,
              "action": "updated"
            }
            """;
        JsonNode node = mapper.readTree(json);

        AtomicReference<FixtureMessage> captured = new AtomicReference<>();
        TypedFeedAdapter adapter = new TypedFeedAdapter(new FeedListener() {
            @Override
            public void onFixture(FixtureMessage message) {
                captured.set(message);
            }
        });

        adapter.accept(node);

        assertNotNull(captured.get());
        assertEquals(99L, captured.get().getMatchId());
    }

    // ============================================================
    //  Scoreboard routing
    // ============================================================

    @Test
    @DisplayName("Scoreboard message is routed to onScoreboard with raw JSON and type")
    void scoreboardMessage_routedToOnScoreboard() throws Exception {
        String json = """
            {
              "type": "scoreboard",
              "scoreboard_type": "cs",
              "id": 1001
            }
            """;
        JsonNode node = mapper.readTree(json);

        AtomicReference<String> capturedType = new AtomicReference<>();
        AtomicReference<JsonNode> capturedRaw = new AtomicReference<>();
        TypedFeedAdapter adapter = new TypedFeedAdapter(new FeedListener() {
            @Override
            public void onScoreboard(JsonNode raw, String scoreboardType) {
                capturedRaw.set(raw);
                capturedType.set(scoreboardType);
            }
        });

        adapter.accept(node);

        assertEquals("cs", capturedType.get());
        assertEquals(1001, capturedRaw.get().get("id").asInt());
    }

    @Test
    @DisplayName("Scoreboard without scoreboard_type field defaults to 'unknown'")
    void scoreboardMessage_missingScoreboardType_defaultsToUnknown() throws Exception {
        String json = """
            {
              "type": "scoreboard",
              "id": 2002
            }
            """;
        JsonNode node = mapper.readTree(json);

        AtomicReference<String> capturedType = new AtomicReference<>();
        TypedFeedAdapter adapter = new TypedFeedAdapter(new FeedListener() {
            @Override
            public void onScoreboard(JsonNode raw, String scoreboardType) {
                capturedType.set(scoreboardType);
            }
        });

        adapter.accept(node);

        assertEquals("unknown", capturedType.get());
    }

    // ============================================================
    //  Unknown type routing
    // ============================================================

    @Test
    @DisplayName("Message with unrecognized type is routed to onUnknown")
    void unknownType_routedToOnUnknown() throws Exception {
        String json = """
            {
              "type": "something_new",
              "data": 123
            }
            """;
        JsonNode node = mapper.readTree(json);

        AtomicReference<JsonNode> captured = new AtomicReference<>();
        TypedFeedAdapter adapter = new TypedFeedAdapter(new FeedListener() {
            @Override
            public void onUnknown(JsonNode raw) {
                captured.set(raw);
            }
        });

        adapter.accept(node);

        assertNotNull(captured.get());
        assertEquals("something_new", captured.get().get("type").asText());
    }

    @Test
    @DisplayName("Message without type field is routed to onUnknown")
    void noTypeField_routedToOnUnknown() throws Exception {
        String json = """
            {
              "data": "no type here"
            }
            """;
        JsonNode node = mapper.readTree(json);

        AtomicReference<JsonNode> captured = new AtomicReference<>();
        TypedFeedAdapter adapter = new TypedFeedAdapter(new FeedListener() {
            @Override
            public void onUnknown(JsonNode raw) {
                captured.set(raw);
            }
        });

        adapter.accept(node);

        assertNotNull(captured.get());
    }

    // ============================================================
    //  Error handling
    // ============================================================

    @Test
    @DisplayName("Deserialization failure falls back to onUnknown")
    void deserializationFailure_fallsToOnUnknown() throws Exception {
        // A markets message with an invalid field type — action should be a string
        // but the model expects an enum, so this may fail depending on strictness.
        // Use a listener that throws on onMarkets to simulate deserialization error.
        String json = """
            {
              "type": "markets",
              "match_id": 1
            }
            """;
        JsonNode node = mapper.readTree(json);

        AtomicReference<JsonNode> unknownCapture = new AtomicReference<>();
        AtomicReference<MarketsMessage> marketsCapture = new AtomicReference<>();
        TypedFeedAdapter adapter = new TypedFeedAdapter(new FeedListener() {
            @Override
            public void onMarkets(MarketsMessage message) {
                marketsCapture.set(message);
                throw new RuntimeException("Simulated processing error");
            }
            @Override
            public void onUnknown(JsonNode raw) {
                unknownCapture.set(raw);
            }
        });

        // The adapter catches the listener exception and falls back to onUnknown
        adapter.accept(node);

        // onMarkets was called (deserialization succeeded) but threw
        assertNotNull(marketsCapture.get());
        // onUnknown should have been called as fallback
        assertNotNull(unknownCapture.get());
    }

    // ============================================================
    //  Constructor validation
    // ============================================================

    @Test
    @DisplayName("Null listener throws IllegalArgumentException")
    void nullListener_throws() {
        assertThrows(IllegalArgumentException.class, () -> new TypedFeedAdapter(null));
    }
}
