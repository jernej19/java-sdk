package com.pandascore.sdk.model.feed.scoreboard;

import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Scoreboard message for Dota 2 matches.
 */
@Data
public class ScoreboardDota2 {
    /** Unique identifier for the scoreboard. */
    private long id;
    /** Time when the scoreboard was last updated. */
    private Instant updatedAt;
    /** Games information for the Dota&nbsp;2 match. */
    private List<Dota2Game> games;
    /** Scoreboard type as reported by the feed. */
    private String scoreboardType;

    @Data
    public static class Dota2Game {
        /** Game identifier. */
        private long id;
        /** Order of the game within the match. */
        private int position;
        /** Status of the game (e.g. running, finished). */
        private String status;
        /** Current gold lead for the radiant side. */
        private int radiantGoldLead;
        /** Optional timer in seconds. */
        private Optional<Integer> timer;
        /** Teams participating in the game. */
        private List<Dota2Team> teams;
    }

    @Data
    public static class Dota2Team {
        /** Identifier of the team. */
        private long id;
        /** Side of the map played by this team. */
        private String side;
        /** Heroes selected by the team. */
        private List<Hero> heroes;
        /** Number of kills by this team. */
        private int kills;
        /** Number of towers destroyed. */
        private int towersDestroyed;
    }

    @Data
    public static class Hero {
        /** Hero identifier. */
        private long id;
        /** Hero name. */
        private String name;
    }
}
