package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScoreboardEbasketball deserialization.
 */
class ScoreboardEbasketballTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    @DisplayName("Deserialize full eBasketball scoreboard")
    void deserialize_fullPayload() throws Exception {
        String json = """
            {
              "id": 4001,
              "updated_at": "2025-05-22T14:00:00Z",
              "scoreboard_type": "ebasketball",
              "games": [
                {
                  "id": 4101,
                  "position": 1,
                  "status": "running",
                  "timer": {"timer": 180, "paused": false, "issued_at": "2025-05-22T13:57:00Z"},
                  "current_quarter": 2,
                  "players": [
                    {"id": 10, "point_score": 35},
                    {"id": 20, "point_score": 28}
                  ],
                  "quarters": [
                    {
                      "index": 1,
                      "scores": [
                        {"id": 10, "point_score": 18},
                        {"id": 20, "point_score": 15}
                      ]
                    },
                    {
                      "index": 2,
                      "scores": [
                        {"id": 10, "point_score": 17},
                        {"id": 20, "point_score": 13}
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        ScoreboardEbasketball sb = mapper.readValue(json, ScoreboardEbasketball.class);

        assertEquals(4001L, sb.getId());
        assertNotNull(sb.getUpdatedAt());
        assertEquals("ebasketball", sb.getScoreboardType());
        assertEquals(1, sb.getGames().size());

        ScoreboardEbasketball.EbasketballGame game = sb.getGames().get(0);
        assertEquals(4101L, game.getId());
        assertEquals("running", game.getStatus());
        assertEquals(2, game.getCurrentQuarter());

        // Timer (counts down)
        assertNotNull(game.getTimer());
        assertEquals(180, game.getTimer().getTimer());
        assertFalse(game.getTimer().getPaused());

        // Players
        assertEquals(2, game.getPlayers().size());
        assertEquals(35, game.getPlayers().get(0).getPointScore());
        assertEquals(28, game.getPlayers().get(1).getPointScore());

        // Quarters
        assertEquals(2, game.getQuarters().size());
        assertEquals(1, game.getQuarters().get(0).getIndex());
        assertEquals(2, game.getQuarters().get(0).getScores().size());
    }

    @Test
    @DisplayName("Deserialize eBasketball scoreboard with empty games")
    void deserialize_emptyGames() throws Exception {
        String json = """
            {"id": 999, "updated_at": "2025-05-22T14:00:00Z", "games": []}
            """;
        ScoreboardEbasketball sb = mapper.readValue(json, ScoreboardEbasketball.class);
        assertTrue(sb.getGames().isEmpty());
    }

    @Test
    @DisplayName("Deserialize eBasketball game with paused timer")
    void deserialize_pausedTimer() throws Exception {
        String json = """
            {
              "id": 1,
              "updated_at": "2025-05-22T14:00:00Z",
              "games": [{
                "id": 10, "position": 1, "status": "running",
                "current_quarter": 3,
                "timer": {"timer": 250, "paused": true, "issued_at": "2025-05-22T14:00:00Z"},
                "players": [],
                "quarters": []
              }]
            }
            """;
        ScoreboardEbasketball sb = mapper.readValue(json, ScoreboardEbasketball.class);
        assertTrue(sb.getGames().get(0).getTimer().getPaused());
        assertEquals(250, sb.getGames().get(0).getTimer().getTimer());
    }
}
