package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FixtureMessage and nested model deserialization.
 */
class FixtureMessageTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Deserialize full fixture message")
    void deserialize_fullMessage() throws Exception {
        String json = """
            {
              "type": "fixture",
              "at": "2025-05-22T14:00:00Z",
              "action": "updated",
              "event_type": "match",
              "event_id": 1000,
              "videogame_slug": "league-of-legends",
              "tournament_tier": "a",
              "match_id": 2000,
              "game_position": 2,
              "match": {
                "id": 2000,
                "name": "T1 vs G2",
                "slug": "t1-vs-g2",
                "status": "live",
                "number_of_games": 5,
                "draw": false,
                "forfeit": false,
                "opponents": [
                  {"type": "Team", "opponent": {"id": 10, "name": "T1", "slug": "t1"}},
                  {"type": "Team", "opponent": {"id": 20, "name": "G2", "slug": "g2"}}
                ],
                "results": [
                  {"team_id": 10, "score": 2},
                  {"team_id": 20, "score": 1}
                ]
              }
            }
            """;

        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);

        assertEquals("fixture", msg.getType());
        assertEquals("2025-05-22T14:00:00Z", msg.getAt());
        assertEquals(FixtureAction.updated, msg.getAction());
        assertEquals("match", msg.getEventType());
        assertEquals(1000L, msg.getEventId());
        assertEquals("league-of-legends", msg.getVideogameSlug());
        assertEquals("a", msg.getTournamentTier());
        assertEquals(2000L, msg.getMatchId());
        assertEquals(2, msg.getGamePosition());

        // Match
        FixtureMatch match = msg.getMatch();
        assertNotNull(match);
        assertEquals(2000L, match.getId());
        assertEquals("T1 vs G2", match.getName());
        assertEquals("t1-vs-g2", match.getSlug());
        assertEquals("live", match.getStatus());
        assertEquals(5, match.getNumberOfGames());
        assertFalse(match.getDraw());
        assertFalse(match.getForfeit());

        // Opponents
        assertEquals(2, match.getOpponents().size());
        assertEquals("T1", match.getOpponents().get(0).getOpponent().getName());
        assertEquals("G2", match.getOpponents().get(1).getOpponent().getName());

        // Results
        assertEquals(2, match.getResults().size());
    }

    @Test
    @DisplayName("Deserialize fixture message with minimal fields")
    void deserialize_minimalMessage() throws Exception {
        String json = "{\"type\": \"fixture\"}";
        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        assertEquals("fixture", msg.getType());
        assertNull(msg.getAction());
        assertNull(msg.getMatch());
        assertNull(msg.getSerie());
    }

    @Test
    @DisplayName("Deserialize fixture match with betting metadata")
    void deserialize_bettingMetadata() throws Exception {
        String json = """
            {
              "type": "fixture",
              "match": {
                "id": 500,
                "betting_metadata": {
                  "betbuilder_enabled": true,
                  "bookable": true,
                  "booked": true,
                  "coverage": "live",
                  "live_available": true,
                  "markets_created": true,
                  "micromarkets_enabled": false,
                  "pandascore_reviewed": true,
                  "settled": false,
                  "inputs_enable": true
                }
              }
            }
            """;

        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        FixtureMatchBettingMetadata meta = msg.getMatch().getBettingMetadata();
        assertNotNull(meta);
        assertTrue(meta.isBetbuilderEnabled());
        assertTrue(meta.isBookable());
        assertTrue(meta.isBooked());
        assertEquals("live", meta.getCoverage());
        assertTrue(meta.isLiveAvailable());
        assertTrue(meta.isMarketsCreated());
        assertFalse(meta.isMicromarketsEnabled());
        assertTrue(meta.isPandascoreReviewed());
        assertFalse(meta.isSettled());
        assertTrue(meta.isInputsEnable());
    }

    @Test
    @DisplayName("Deserialize fixture match with games")
    void deserialize_matchWithGames() throws Exception {
        String json = """
            {
              "type": "fixture",
              "match": {
                "id": 600,
                "games": [
                  {
                    "id": 601,
                    "position": 1,
                    "status": "finished",
                    "finished": true,
                    "winner": {"id": 10, "type": "Team"}
                  }
                ]
              }
            }
            """;

        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        assertEquals(1, msg.getMatch().getGames().size());
        Game game = msg.getMatch().getGames().get(0);
        assertEquals(601L, game.getId());
        assertEquals(1, game.getPosition());
        assertEquals("finished", game.getStatus());
        assertTrue(game.getFinished());
        assertEquals(10L, game.getWinner().getId());
        assertEquals("Team", game.getWinner().getType());
    }

    @Test
    @DisplayName("Deserialize fixture match with league and tournament")
    void deserialize_leagueAndTournament() throws Exception {
        String json = """
            {
              "type": "fixture",
              "match": {
                "id": 700,
                "league": {"id": 10, "name": "LEC", "slug": "lec"},
                "tournament": {
                  "id": 20,
                  "name": "LEC Spring 2025",
                  "slug": "lec-spring-2025",
                  "tier": "a"
                },
                "videogame": {"id": 1, "name": "League of Legends", "slug": "lol"}
              }
            }
            """;

        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        FixtureMatch match = msg.getMatch();

        assertEquals("LEC", match.getLeague().getName());
        assertEquals("lec", match.getLeague().getSlug());
        assertEquals("LEC Spring 2025", match.getTournament().getName());
        assertEquals("a", match.getTournament().getTier());
        assertEquals("League of Legends", match.getVideogame().getName());
    }

    @Test
    @DisplayName("Deserialize fixture with serie")
    void deserialize_serie() throws Exception {
        String json = """
            {
              "type": "fixture",
              "serie": {
                "id": 50,
                "full_name": "Spring 2025",
                "league_name": "LEC",
                "season": "Spring",
                "year": 2025,
                "tier": "a"
              }
            }
            """;

        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        FixtureSerie serie = msg.getSerie();
        assertNotNull(serie);
        assertEquals(50L, serie.getId());
        assertEquals("Spring 2025", serie.getFullName());
        assertEquals("LEC", serie.getLeagueName());
        assertEquals("Spring", serie.getSeason());
        assertEquals(2025, serie.getYear());
    }

    @Test
    @DisplayName("Unknown fields are ignored in fixture models")
    void deserialize_unknownFieldsIgnored() throws Exception {
        String json = """
            {
              "type": "fixture",
              "action": "booked",
              "some_new_field": "value",
              "match": {"id": 800, "brand_new_field": 42}
            }
            """;
        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        assertEquals(FixtureAction.booked, msg.getAction());
        assertEquals(800L, msg.getMatch().getId());
    }

    @Test
    @DisplayName("Deserialize fixture match with timestamps")
    void deserialize_timestamps() throws Exception {
        String json = """
            {
              "type": "fixture",
              "match": {
                "id": 900,
                "scheduled_at": "2025-06-01T18:00:00Z",
                "begin_at": "2025-06-01T18:05:00Z",
                "end_at": "2025-06-01T20:00:00Z",
                "modified_at": "2025-06-01T20:01:00Z",
                "original_scheduled_at": "2025-06-01T17:00:00Z"
              }
            }
            """;

        FixtureMatch match = mapper.readValue(json, FixtureMessage.class).getMatch();
        assertEquals("2025-06-01T18:00:00Z", match.getScheduledAt());
        assertEquals("2025-06-01T18:05:00Z", match.getBeginAt());
        assertEquals("2025-06-01T20:00:00Z", match.getEndAt());
        assertEquals("2025-06-01T20:01:00Z", match.getModifiedAt());
        assertEquals("2025-06-01T17:00:00Z", match.getOriginalScheduledAt());
    }
}
