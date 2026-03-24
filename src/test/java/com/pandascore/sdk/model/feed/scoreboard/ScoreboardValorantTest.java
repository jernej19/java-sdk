package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScoreboardValorant deserialization.
 */
class ScoreboardValorantTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    @DisplayName("Deserialize full Valorant scoreboard")
    void deserialize_fullPayload() throws Exception {
        String json = """
            {
              "id": 9001,
              "updated_at": "2025-05-22T14:00:00Z",
              "scoreboard_type": "valorant",
              "games": [
                {
                  "id": 9101,
                  "position": 1,
                  "status": "running",
                  "teams": [
                    {"id": 100, "round_score": 12, "side": "attack"},
                    {"id": 200, "round_score": 10, "side": "defense"}
                  ]
                }
              ]
            }
            """;

        ScoreboardValorant sb = mapper.readValue(json, ScoreboardValorant.class);

        assertEquals(9001L, sb.getId());
        assertNotNull(sb.getUpdatedAt());
        assertEquals("valorant", sb.getScoreboardType());
        assertEquals(1, sb.getGames().size());

        ScoreboardValorant.ValorantGame game = sb.getGames().get(0);
        assertEquals(9101L, game.getId());
        assertEquals(1, game.getPosition());
        assertEquals("running", game.getStatus());

        assertEquals(2, game.getTeams().size());
        assertEquals(12, game.getTeams().get(0).getRoundScore());
        assertEquals("attack", game.getTeams().get(0).getSide());
        assertEquals(10, game.getTeams().get(1).getRoundScore());
        assertEquals("defense", game.getTeams().get(1).getSide());
    }

    @Test
    @DisplayName("Deserialize Valorant scoreboard with empty games")
    void deserialize_emptyGames() throws Exception {
        String json = """
            {"id": 999, "updated_at": "2025-05-22T14:00:00Z", "games": []}
            """;
        ScoreboardValorant sb = mapper.readValue(json, ScoreboardValorant.class);
        assertTrue(sb.getGames().isEmpty());
    }

    @Test
    @DisplayName("Deserialize Valorant game without map field")
    void deserialize_noMapField() throws Exception {
        String json = """
            {
              "id": 1,
              "updated_at": "2025-05-22T14:00:00Z",
              "games": [{
                "id": 10, "position": 1, "status": "not_started",
                "teams": []
              }]
            }
            """;
        ScoreboardValorant sb = mapper.readValue(json, ScoreboardValorant.class);
        assertNotNull(sb.getGames().get(0));
    }
}
