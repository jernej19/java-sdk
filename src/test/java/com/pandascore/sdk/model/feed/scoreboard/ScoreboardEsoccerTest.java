package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScoreboardEsoccer deserialization.
 */
class ScoreboardEsoccerTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    @DisplayName("Deserialize full eSoccer scoreboard")
    void deserialize_fullPayload() throws Exception {
        String json = """
            {
              "id": 3001,
              "updated_at": "2025-05-22T14:00:00Z",
              "scoreboard_type": "esoccer",
              "games": [
                {
                  "id": 3101,
                  "position": 1,
                  "status": "running",
                  "timer": {"timer": 300, "paused": false, "issued_at": "2025-05-22T13:55:00Z"},
                  "current_half": 1,
                  "players": [
                    {"id": 10, "goal_score": 2},
                    {"id": 20, "goal_score": 1}
                  ],
                  "halves": [
                    {
                      "index": 1,
                      "scores": [
                        {"id": 10, "goal_score": 2},
                        {"id": 20, "goal_score": 1}
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        ScoreboardEsoccer sb = mapper.readValue(json, ScoreboardEsoccer.class);

        assertEquals(3001L, sb.getId());
        assertNotNull(sb.getUpdatedAt());
        assertEquals("esoccer", sb.getScoreboardType());
        assertEquals(1, sb.getGames().size());

        ScoreboardEsoccer.EsoccerGame game = sb.getGames().get(0);
        assertEquals(3101L, game.getId());
        assertEquals(1, game.getPosition());
        assertEquals("running", game.getStatus());
        assertEquals(1, game.getCurrentHalf());

        // Timer
        assertNotNull(game.getTimer());
        assertEquals(300, game.getTimer().getTimer());
        assertFalse(game.getTimer().getPaused());

        // Players
        assertEquals(2, game.getPlayers().size());
        assertEquals(10L, game.getPlayers().get(0).getId());
        assertEquals(2, game.getPlayers().get(0).getGoalScore());

        // Halves
        assertEquals(1, game.getHalves().size());
        assertEquals(1, game.getHalves().get(0).getIndex());
        assertEquals(2, game.getHalves().get(0).getScores().size());
    }

    @Test
    @DisplayName("Deserialize eSoccer scoreboard with empty games")
    void deserialize_emptyGames() throws Exception {
        String json = """
            {"id": 999, "updated_at": "2025-05-22T14:00:00Z", "games": []}
            """;
        ScoreboardEsoccer sb = mapper.readValue(json, ScoreboardEsoccer.class);
        assertTrue(sb.getGames().isEmpty());
    }

    @Test
    @DisplayName("Deserialize eSoccer with second half")
    void deserialize_secondHalf() throws Exception {
        String json = """
            {
              "id": 1,
              "updated_at": "2025-05-22T14:00:00Z",
              "games": [{
                "id": 10, "position": 1, "status": "running",
                "current_half": 2,
                "timer": {"timer": 500, "paused": false, "issued_at": "2025-05-22T14:00:00Z"},
                "players": [],
                "halves": [
                  {"index": 1, "scores": [{"id": 1, "goal_score": 1}]},
                  {"index": 2, "scores": [{"id": 1, "goal_score": 2}]}
                ]
              }]
            }
            """;
        ScoreboardEsoccer sb = mapper.readValue(json, ScoreboardEsoccer.class);
        assertEquals(2, sb.getGames().get(0).getCurrentHalf());
        assertEquals(2, sb.getGames().get(0).getHalves().size());
    }
}
