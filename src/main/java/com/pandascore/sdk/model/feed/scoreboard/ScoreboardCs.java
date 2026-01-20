package com.pandascore.sdk.model.feed.scoreboard;

import lombok.Data;
import java.time.Instant;
import java.util.List;

/**
 * Scoreboard message for Counter‑Strike matches.
 */
@Data
public class ScoreboardCs {
    /** Unique identifier for the scoreboard message. */
    private long id;
    /** Timestamp at which this scoreboard was last updated. */
    private Instant updatedAt;
    /** Games that compose the scoreboard for the match. */
    private List<CsGame> games;
    /** Type of scoreboard coming from the feed. */
    private String scoreboardType;

    @Data
    public static class CsGame {
        /** Game identifier. */
        private long id;
        /** Position of the game within the match. */
        private int position;
        /** Current status of the game. */
        private String status;
        /** Map being played for this game. */
        private CsMap map;
        /** Teams participating in this game. */
        private List<CsTeam> teams;
    }

    @Data
    public static class CsMap {
        /** Map identifier. */
        private long id;
        /** Human readable name of the map. */
        private String name;
    }

    @Data
    public static class CsTeam {
        /** Identifier of the team. */
        private long id;
        /** Side chosen by the team (CT/T). */
        private String side;
        /** Current score in rounds for the game. */
        private int roundScore;
    }
}
