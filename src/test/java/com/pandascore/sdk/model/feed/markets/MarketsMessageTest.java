package com.pandascore.sdk.model.feed.markets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MarketsMessage deserialization.
 */
class MarketsMessageTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Deserialize full markets message with all fields")
    void deserialize_fullMessage() throws Exception {
        String json = """
            {
              "type": "markets",
              "at": "2025-05-22T14:00:00Z",
              "action": "odds_changed",
              "event_type": "match",
              "event_id": 12345,
              "videogame_slug": "cs-go",
              "match_id": 6789,
              "tournament_tier": "s",
              "game_position": 1,
              "markets": [
                {
                  "id": "mkt-001",
                  "name": "Match Winner",
                  "status": "active",
                  "template": "match_winner",
                  "overround": 1.05,
                  "margin": 0.05
                }
              ]
            }
            """;

        MarketsMessage msg = mapper.readValue(json, MarketsMessage.class);

        assertEquals("markets", msg.getType());
        assertEquals("2025-05-22T14:00:00Z", msg.getAt());
        assertEquals(MarketAction.odds_changed, msg.getAction());
        assertEquals("match", msg.getEventType());
        assertEquals(12345L, msg.getEventId());
        assertEquals("cs-go", msg.getVideogameSlug());
        assertEquals(6789L, msg.getMatchId());
        assertEquals("s", msg.getTournamentTier());
        assertEquals(1, msg.getGamePosition());
        assertEquals(1, msg.getMarkets().size());

        MarketsMessageMarket market = msg.getMarkets().get(0);
        assertEquals("mkt-001", market.getId());
        assertEquals("Match Winner", market.getName());
        assertEquals("active", market.getStatus());
        assertEquals("match_winner", market.getTemplate());
        assertEquals(1.05, market.getOverround(), 0.001);
        assertEquals(0.05, market.getMargin(), 0.001);
    }

    @Test
    @DisplayName("Deserialize markets message with minimal fields")
    void deserialize_minimalMessage() throws Exception {
        String json = "{\"type\": \"markets\", \"action\": \"created\"}";
        MarketsMessage msg = mapper.readValue(json, MarketsMessage.class);

        assertEquals("markets", msg.getType());
        assertEquals(MarketAction.created, msg.getAction());
        assertNull(msg.getAt());
        assertNull(msg.getEventType());
        assertNull(msg.getEventId());
        assertNull(msg.getMarkets());
    }

    @Test
    @DisplayName("Unknown fields are ignored (JsonIgnoreProperties)")
    void deserialize_unknownFieldsIgnored() throws Exception {
        String json = """
            {
              "type": "markets",
              "action": "settled",
              "unknown_field": "some_value",
              "another_unknown": 42
            }
            """;
        MarketsMessage msg = mapper.readValue(json, MarketsMessage.class);
        assertEquals("markets", msg.getType());
        assertEquals(MarketAction.settled, msg.getAction());
    }

    @Test
    @DisplayName("Deserialize all MarketAction values")
    void deserialize_allMarketActions() throws Exception {
        for (MarketAction action : MarketAction.values()) {
            String json = "{\"action\": \"" + action.name() + "\"}";
            MarketsMessage msg = mapper.readValue(json, MarketsMessage.class);
            assertEquals(action, msg.getAction());
        }
    }

    @Test
    @DisplayName("Deserialize market with selections")
    void deserialize_marketWithSelections() throws Exception {
        String json = """
            {
              "type": "markets",
              "action": "odds_changed",
              "markets": [{
                "id": "m1",
                "selections": [
                  {"id": "s1", "name": "Team A", "odds_decimal": 1.5, "probability": 0.65},
                  {"id": "s2", "name": "Team B", "odds_decimal": 2.5, "probability": 0.35}
                ]
              }]
            }
            """;
        MarketsMessage msg = mapper.readValue(json, MarketsMessage.class);
        assertEquals(2, msg.getMarkets().get(0).getSelections().size());
        assertEquals("s1", msg.getMarkets().get(0).getSelections().get(0).getId());
        assertEquals("Team A", msg.getMarkets().get(0).getSelections().get(0).getName());
    }

    @Test
    @DisplayName("Deserialize market with game-specific fields")
    void deserialize_gameSpecificFields() throws Exception {
        String json = """
            {
              "type": "markets",
              "markets": [{
                "id": "m1",
                "drake_index": 2,
                "nashor_index": 1,
                "tower_index": 3,
                "rift_herald_index": 0,
                "champion_id": 55,
                "timer": 120,
                "round_index": 5,
                "handicap_home": 1.5,
                "handicap_away": -1.5
              }]
            }
            """;
        MarketsMessage msg = mapper.readValue(json, MarketsMessage.class);
        MarketsMessageMarket m = msg.getMarkets().get(0);
        assertEquals(2, m.getDrakeIndex());
        assertEquals(1, m.getNashorIndex());
        assertEquals(3, m.getTowerIndex());
        assertEquals(0, m.getRiftHeraldIndex());
        assertEquals(55L, m.getChampionId());
        assertEquals(120, m.getTimer());
        assertEquals(5, m.getRoundIndex());
        assertEquals(1.5, m.getHandicapHome(), 0.001);
        assertEquals(-1.5, m.getHandicapAway(), 0.001);
    }
}
