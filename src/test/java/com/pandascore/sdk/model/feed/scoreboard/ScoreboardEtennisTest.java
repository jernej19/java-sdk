package com.pandascore.sdk.model.feed.scoreboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardEtennisTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    void testDeserialize_fullPayload() throws Exception {
        String json = """
                {
                  "id": 12345,
                  "updated_at": "2022-03-15T10:00:48.109Z",
                  "sets": [
                    {
                      "id": 100,
                      "position": 1,
                      "status": "finished",
                      "current_game": 6,
                      "players": [
                        { "id": 1, "set_score": 6 },
                        { "id": 2, "set_score": 4 }
                      ],
                      "games": [
                        {
                          "index": 1,
                          "server": 1,
                          "players": [
                            { "id": 1, "game_score": 40 },
                            { "id": 2, "game_score": 15 }
                          ]
                        },
                        {
                          "index": 2,
                          "server": 2,
                          "players": [
                            { "id": 1, "game_score": "adv" },
                            { "id": 2, "game_score": 40 }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        ScoreboardEtennis scoreboard = mapper.readValue(json, ScoreboardEtennis.class);

        assertEquals(12345, scoreboard.getId());
        assertNotNull(scoreboard.getUpdatedAt());
        assertEquals(1, scoreboard.getSets().size());

        ScoreboardEtennis.EtennisSet set = scoreboard.getSets().get(0);
        assertEquals(100, set.getId());
        assertEquals(1, set.getPosition());
        assertEquals("finished", set.getStatus());
        assertEquals(6, set.getCurrentGame());

        // Set players
        assertEquals(2, set.getPlayers().size());
        assertEquals(1, set.getPlayers().get(0).getId());
        assertEquals(6, set.getPlayers().get(0).getSetScore());
        assertEquals(2, set.getPlayers().get(1).getId());
        assertEquals(4, set.getPlayers().get(1).getSetScore());

        // Games
        assertEquals(2, set.getGames().size());

        ScoreboardEtennis.EtennisGame game1 = set.getGames().get(0);
        assertEquals(1, game1.getIndex());
        assertEquals(1, game1.getServer());
        assertEquals(40, game1.getPlayers().get(0).getGameScore());
        assertEquals(15, game1.getPlayers().get(1).getGameScore());

        // "adv" string score
        ScoreboardEtennis.EtennisGame game2 = set.getGames().get(1);
        assertEquals("adv", game2.getPlayers().get(0).getGameScore());
        assertEquals(40, game2.getPlayers().get(1).getGameScore());
    }

    @Test
    void testDeserialize_emptySets() throws Exception {
        String json = """
                {
                  "id": 999,
                  "updated_at": "2022-03-15T10:00:48.109Z",
                  "sets": []
                }
                """;

        ScoreboardEtennis scoreboard = mapper.readValue(json, ScoreboardEtennis.class);
        assertEquals(999, scoreboard.getId());
        assertTrue(scoreboard.getSets().isEmpty());
    }

    @Test
    void testDeserialize_gameScoreZero() throws Exception {
        String json = """
                {
                  "id": 1,
                  "updated_at": "2022-03-15T10:00:48.109Z",
                  "sets": [{
                    "id": 10,
                    "position": 1,
                    "status": "running",
                    "current_game": 1,
                    "players": [],
                    "games": [{
                      "index": 1,
                      "server": 1,
                      "players": [
                        { "id": 1, "game_score": 0 },
                        { "id": 2, "game_score": 0 }
                      ]
                    }]
                  }]
                }
                """;

        ScoreboardEtennis scoreboard = mapper.readValue(json, ScoreboardEtennis.class);
        ScoreboardEtennis.EtennisGamePlayer player = scoreboard.getSets().get(0).getGames().get(0).getPlayers().get(0);
        assertEquals(0, player.getGameScore());
    }
}
