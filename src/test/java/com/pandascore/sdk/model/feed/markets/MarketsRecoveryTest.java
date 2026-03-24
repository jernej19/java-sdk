package com.pandascore.sdk.model.feed.markets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MarketsRecoveryMatch and MarketsRecoveryGame deserialization.
 */
class MarketsRecoveryTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Deserialize recovery match with markets and games")
    void deserialize_fullRecoveryMatch() throws Exception {
        String json = """
            {
              "id": 100,
              "markets": [
                {"id": "m1", "name": "Winner", "status": "active"},
                {"id": "m2", "name": "Over/Under", "status": "suspended"}
              ],
              "games": [
                {
                  "id": 10,
                  "position": 1,
                  "markets": [
                    {"id": "gm1", "name": "Game Winner"}
                  ]
                },
                {
                  "id": 11,
                  "position": 2,
                  "markets": []
                }
              ]
            }
            """;

        MarketsRecoveryMatch match = mapper.readValue(json, MarketsRecoveryMatch.class);

        assertEquals(100L, match.getId());
        assertEquals(2, match.getMarkets().size());
        assertEquals("m1", match.getMarkets().get(0).getId());
        assertEquals("m2", match.getMarkets().get(1).getId());

        assertEquals(2, match.getGames().size());
        MarketsRecoveryGame game1 = match.getGames().get(0);
        assertEquals(10L, game1.getId());
        assertEquals(1, game1.getPosition());
        assertEquals(1, game1.getMarkets().size());
        assertEquals("gm1", game1.getMarkets().get(0).getId());

        MarketsRecoveryGame game2 = match.getGames().get(1);
        assertEquals(11L, game2.getId());
        assertEquals(2, game2.getPosition());
        assertTrue(game2.getMarkets().isEmpty());
    }

    @Test
    @DisplayName("Deserialize recovery match with null lists")
    void deserialize_nullLists() throws Exception {
        String json = "{\"id\": 200}";
        MarketsRecoveryMatch match = mapper.readValue(json, MarketsRecoveryMatch.class);
        assertEquals(200L, match.getId());
        assertNull(match.getMarkets());
        assertNull(match.getGames());
    }

    @Test
    @DisplayName("Unknown fields are ignored in recovery models")
    void deserialize_unknownFieldsIgnored() throws Exception {
        String json = "{\"id\": 300, \"unknown_field\": true}";
        MarketsRecoveryMatch match = mapper.readValue(json, MarketsRecoveryMatch.class);
        assertEquals(300L, match.getId());
    }

    @Test
    @DisplayName("MarketsRecoveryGame deserialization")
    void deserialize_recoveryGame() throws Exception {
        String json = """
            {
              "id": 50,
              "position": 3,
              "markets": [{"id": "gm10", "status": "active"}]
            }
            """;
        MarketsRecoveryGame game = mapper.readValue(json, MarketsRecoveryGame.class);
        assertEquals(50L, game.getId());
        assertEquals(3, game.getPosition());
        assertEquals(1, game.getMarkets().size());
    }
}
