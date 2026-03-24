package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScoreboardLol deserialization.
 */
class ScoreboardLolTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    @DisplayName("Deserialize full LoL scoreboard")
    void deserialize_fullPayload() throws Exception {
        String json = """
            {
              "id": 7001,
              "updated_at": "2025-05-22T14:00:00Z",
              "scoreboard_type": "lol",
              "games": [
                {
                  "id": 8001,
                  "position": 1,
                  "status": "running",
                  "draft_phase": "ban_phase_1",
                  "teams": [
                    {
                      "id": 100,
                      "side": "blue",
                      "kills": 10,
                      "drakes": 2,
                      "inhibitors": 0,
                      "nashors": 0,
                      "towers": 4
                    },
                    {
                      "id": 200,
                      "side": "red",
                      "kills": 8,
                      "drakes": 1,
                      "inhibitors": 0,
                      "nashors": 1,
                      "towers": 2
                    }
                  ]
                }
              ]
            }
            """;

        ScoreboardLol sb = mapper.readValue(json, ScoreboardLol.class);

        assertEquals(7001L, sb.getId());
        assertNotNull(sb.getUpdatedAt());
        assertEquals("lol", sb.getScoreboardType());
        assertEquals(1, sb.getGames().size());

        ScoreboardLol.LolGame game = sb.getGames().get(0);
        assertEquals(8001L, game.getId());
        assertEquals(1, game.getPosition());
        assertEquals("running", game.getStatus());
        assertEquals("ban_phase_1", game.getDraftPhase());

        // Teams
        assertEquals(2, game.getTeams().size());
        ScoreboardLol.LolTeam blue = game.getTeams().get(0);
        assertEquals("blue", blue.getSide());
        assertEquals(10, blue.getKills());
        assertEquals(2, blue.getDrakes());
        assertEquals(0, blue.getInhibitors());
        assertEquals(0, blue.getNashors());
        assertEquals(4, blue.getTowers());

        ScoreboardLol.LolTeam red = game.getTeams().get(1);
        assertEquals("red", red.getSide());
        assertEquals(8, red.getKills());
        assertEquals(1, red.getNashors());
    }

    @Test
    @DisplayName("Deserialize LoL scoreboard with empty games")
    void deserialize_emptyGames() throws Exception {
        String json = """
            {"id": 999, "updated_at": "2025-05-22T14:00:00Z", "games": []}
            """;
        ScoreboardLol sb = mapper.readValue(json, ScoreboardLol.class);
        assertTrue(sb.getGames().isEmpty());
    }

    @Test
    @DisplayName("Deserialize LoL game without timer field")
    void deserialize_noTimerField() throws Exception {
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
        ScoreboardLol sb = mapper.readValue(json, ScoreboardLol.class);
        assertNotNull(sb.getGames().get(0));
    }
}
