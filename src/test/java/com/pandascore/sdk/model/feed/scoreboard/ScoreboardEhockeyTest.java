package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScoreboardEhockey deserialization.
 */
class ScoreboardEhockeyTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    @DisplayName("Deserialize full eHockey scoreboard")
    void deserialize_fullPayload() throws Exception {
        String json = """
            {
              "id": 6001,
              "updated_at": "2025-05-22T14:00:00Z",
              "scoreboard_type": "ehockey",
              "games": [
                {
                  "id": 6101,
                  "position": 1,
                  "status": "running",
                  "timer": {"timer": 200, "paused": false, "issued_at": "2025-05-22T13:56:40Z"},
                  "current_period": 2,
                  "players": [
                    {"id": 10, "goal_score": 3},
                    {"id": 20, "goal_score": 2}
                  ],
                  "periods": [
                    {
                      "index": 1,
                      "players": [
                        {"id": 10, "goal_score": 1},
                        {"id": 20, "goal_score": 1}
                      ]
                    },
                    {
                      "index": 2,
                      "players": [
                        {"id": 10, "goal_score": 2},
                        {"id": 20, "goal_score": 1}
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        ScoreboardEhockey sb = mapper.readValue(json, ScoreboardEhockey.class);

        assertEquals(6001L, sb.getId());
        assertNotNull(sb.getUpdatedAt());
        assertEquals("ehockey", sb.getScoreboardType());
        assertEquals(1, sb.getGames().size());

        ScoreboardEhockey.EhockeyGame game = sb.getGames().get(0);
        assertEquals(6101L, game.getId());
        assertEquals(1, game.getPosition());
        assertEquals("running", game.getStatus());
        assertEquals(2, game.getCurrentPeriod());

        // Timer (counts down)
        assertNotNull(game.getTimer());
        assertEquals(200, game.getTimer().getTimer());
        assertFalse(game.getTimer().getPaused());

        // Players
        assertEquals(2, game.getPlayers().size());
        assertEquals(3, game.getPlayers().get(0).getGoalScore());
        assertEquals(2, game.getPlayers().get(1).getGoalScore());

        // Periods
        assertEquals(2, game.getPeriods().size());
        assertEquals(1, game.getPeriods().get(0).getIndex());
        assertEquals(2, game.getPeriods().get(0).getPlayers().size());
    }

    @Test
    @DisplayName("Deserialize eHockey scoreboard with empty games")
    void deserialize_emptyGames() throws Exception {
        String json = """
            {"id": 999, "updated_at": "2025-05-22T14:00:00Z", "games": []}
            """;
        ScoreboardEhockey sb = mapper.readValue(json, ScoreboardEhockey.class);
        assertTrue(sb.getGames().isEmpty());
    }

    @Test
    @DisplayName("Deserialize eHockey game in overtime (period > 3)")
    void deserialize_overtimePeriod() throws Exception {
        String json = """
            {
              "id": 1,
              "updated_at": "2025-05-22T14:00:00Z",
              "games": [{
                "id": 10, "position": 1, "status": "running",
                "current_period": 4,
                "timer": {"timer": 100, "paused": false, "issued_at": "2025-05-22T14:00:00Z"},
                "players": [],
                "periods": [
                  {"index": 1, "players": []},
                  {"index": 2, "players": []},
                  {"index": 3, "players": []},
                  {"index": 4, "players": []}
                ]
              }]
            }
            """;
        ScoreboardEhockey sb = mapper.readValue(json, ScoreboardEhockey.class);
        assertEquals(4, sb.getGames().get(0).getCurrentPeriod());
        assertEquals(4, sb.getGames().get(0).getPeriods().size());
    }
}
