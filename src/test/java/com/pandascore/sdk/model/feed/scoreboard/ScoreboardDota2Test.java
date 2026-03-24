package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScoreboardDota2 deserialization.
 */
class ScoreboardDota2Test {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    @DisplayName("Deserialize full Dota2 scoreboard")
    void deserialize_fullPayload() throws Exception {
        String json = """
            {
              "id": 5001,
              "updated_at": "2025-05-22T14:00:00Z",
              "scoreboard_type": "dota2",
              "games": [
                {
                  "id": 6001,
                  "position": 1,
                  "status": "running",
                  "radiant_gold_lead": 2500,
                  "teams": [
                    {
                      "id": 100,
                      "side": "radiant",
                      "kills": 15,
                      "towers_destroyed": 3,
                      "heroes": [
                        {"id": 1, "name": "Anti-Mage"},
                        {"id": 2, "name": "Crystal Maiden"}
                      ]
                    },
                    {
                      "id": 200,
                      "side": "dire",
                      "kills": 12,
                      "towers_destroyed": 1,
                      "heroes": [
                        {"id": 3, "name": "Pudge"}
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        ScoreboardDota2 sb = mapper.readValue(json, ScoreboardDota2.class);

        assertEquals(5001L, sb.getId());
        assertNotNull(sb.getUpdatedAt());
        assertEquals("dota2", sb.getScoreboardType());
        assertEquals(1, sb.getGames().size());

        ScoreboardDota2.Dota2Game game = sb.getGames().get(0);
        assertEquals(6001L, game.getId());
        assertEquals(1, game.getPosition());
        assertEquals("running", game.getStatus());
        assertEquals(2500, game.getRadiantGoldLead());

        assertEquals(2, game.getTeams().size());
        ScoreboardDota2.Dota2Team radiant = game.getTeams().get(0);
        assertEquals("radiant", radiant.getSide());
        assertEquals(15, radiant.getKills());
        assertEquals(3, radiant.getTowersDestroyed());
        assertEquals(2, radiant.getHeroes().size());
        assertEquals("Anti-Mage", radiant.getHeroes().get(0).getName());

        ScoreboardDota2.Dota2Team dire = game.getTeams().get(1);
        assertEquals("dire", dire.getSide());
        assertEquals(12, dire.getKills());
        assertEquals(1, dire.getHeroes().size());
    }

    @Test
    @DisplayName("Deserialize Dota2 scoreboard with empty games")
    void deserialize_emptyGames() throws Exception {
        String json = """
            {"id": 999, "updated_at": "2025-05-22T14:00:00Z", "games": []}
            """;
        ScoreboardDota2 sb = mapper.readValue(json, ScoreboardDota2.class);
        assertTrue(sb.getGames().isEmpty());
    }

    @Test
    @DisplayName("Deserialize Dota2 game without timer field")
    void deserialize_noTimerField() throws Exception {
        String json = """
            {
              "id": 1,
              "updated_at": "2025-05-22T14:00:00Z",
              "games": [{
                "id": 10, "position": 1, "status": "not_started",
                "radiant_gold_lead": 0, "teams": []
              }]
            }
            """;
        ScoreboardDota2 sb = mapper.readValue(json, ScoreboardDota2.class);
        assertNotNull(sb.getGames().get(0));
    }
}
