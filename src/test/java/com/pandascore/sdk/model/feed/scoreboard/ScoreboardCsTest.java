package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScoreboardCs deserialization.
 */
class ScoreboardCsTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    @DisplayName("Deserialize full CS scoreboard")
    void deserialize_fullPayload() throws Exception {
        String json = """
            {
              "id": 1001,
              "updated_at": "2025-05-22T14:00:00Z",
              "scoreboard_type": "cs",
              "games": [
                {
                  "id": 2001,
                  "position": 1,
                  "status": "finished",
                  "map": {"id": 10, "name": "de_dust2"},
                  "teams": [
                    {"id": 100, "side": "CT", "round_score": 16},
                    {"id": 200, "side": "T", "round_score": 12}
                  ]
                },
                {
                  "id": 2002,
                  "position": 2,
                  "status": "running",
                  "map": {"id": 11, "name": "de_mirage"},
                  "teams": [
                    {"id": 100, "side": "T", "round_score": 8},
                    {"id": 200, "side": "CT", "round_score": 7}
                  ]
                }
              ]
            }
            """;

        ScoreboardCs sb = mapper.readValue(json, ScoreboardCs.class);

        assertEquals(1001L, sb.getId());
        assertNotNull(sb.getUpdatedAt());
        assertEquals("cs", sb.getScoreboardType());
        assertEquals(2, sb.getGames().size());

        ScoreboardCs.CsGame game1 = sb.getGames().get(0);
        assertEquals(2001L, game1.getId());
        assertEquals(1, game1.getPosition());
        assertEquals("finished", game1.getStatus());
        assertEquals("de_dust2", game1.getMap().getName());
        assertEquals(10L, game1.getMap().getId());
        assertEquals(2, game1.getTeams().size());
        assertEquals(100L, game1.getTeams().get(0).getId());
        assertEquals("CT", game1.getTeams().get(0).getSide());
        assertEquals(16, game1.getTeams().get(0).getRoundScore());

        ScoreboardCs.CsGame game2 = sb.getGames().get(1);
        assertEquals("running", game2.getStatus());
        assertEquals("de_mirage", game2.getMap().getName());
    }

    @Test
    @DisplayName("Deserialize CS scoreboard with empty games")
    void deserialize_emptyGames() throws Exception {
        String json = """
            {"id": 999, "updated_at": "2025-05-22T14:00:00Z", "games": []}
            """;
        ScoreboardCs sb = mapper.readValue(json, ScoreboardCs.class);
        assertEquals(999L, sb.getId());
        assertTrue(sb.getGames().isEmpty());
    }

    @Test
    @DisplayName("Deserialize CS game with no teams")
    void deserialize_gameWithNoTeams() throws Exception {
        String json = """
            {
              "id": 1,
              "updated_at": "2025-05-22T14:00:00Z",
              "games": [{"id": 10, "position": 1, "status": "not_started", "teams": []}]
            }
            """;
        ScoreboardCs sb = mapper.readValue(json, ScoreboardCs.class);
        assertTrue(sb.getGames().get(0).getTeams().isEmpty());
    }
}
