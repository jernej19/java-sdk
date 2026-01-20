package com.pandascore.sdk.model.feed.scoreboard;

import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Scoreboard message for League of Legends matches.
 */
@Data
public class ScoreboardLol {
    /** Identifier of the scoreboard. */
    private long id;
    /** Last time the scoreboard was updated. */
    private Instant updatedAt;
    /** Games contained in the match scoreboard. */
    private List<LolGame> games;
    /** Scoreboard type from the feed. */
    private String scoreboardType;

    @Data
    public static class LolGame {
        /** Game identifier. */
        private long id;
        /** Game position within the series. */
        private int position;
        /** Status of the game. */
        private String status;
        /** Countdown timer for the game if available. */
        private Optional<LolTimerObject> timer;
        /** Teams participating in the game. */
        private List<LolTeam> teams;
        /** Current draft phase. */
        private String draftPhase;
    }

    @Data
    public static class LolTeam {
        /** Team identifier. */
        private long id;
        /** Map side for the team. */
        private String side;
        /** Number of kills. */
        private int kills;
        /** Number of drakes killed. */
        private int drakes;
        /** Number of inhibitors destroyed. */
        private int inhibitors;
        /** Number of Nashor kills. */
        private int nashors;
        /** Towers destroyed by the team. */
        private int towers;
    }

    @Data
    public static class LolTimerObject {
        /** Current timer value in seconds. */
        private int timer;
        /** Whether the timer is currently paused. */
        private boolean paused;
        /** Timestamp when the timer was issued. */
        private Instant issuedAt;
    }
}
