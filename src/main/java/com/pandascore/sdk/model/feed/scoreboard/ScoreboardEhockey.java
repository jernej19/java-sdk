package com.pandascore.sdk.model.feed.scoreboard;

import lombok.Data;
import java.time.Instant;
import java.util.List;

/**
 * Scoreboard message for eHockey matches.
 */
@Data
public class ScoreboardEhockey {
    /** Identifier of the scoreboard. */
    private Long id;
    /** Last update time of this scoreboard. */
    private Instant updatedAt;
    /** List of games that make up the match. */
    private List<EhockeyGame> games;
    /** Type of scoreboard emitted by the feed. */
    private String scoreboardType;

    @Data
    public static class EhockeyGame {
        /** Game identifier. */
        private Long id;
        /** Game number within the match. */
        private Integer position;
        /** Game status. */
        private String status;
        /** Match timer object (counts DOWN: 300→0 each period). */
        private EhockeyTimerObject timer;
        /** Current period (1-3 regulation, >3 OT). */
        private Integer currentPeriod;
        /** Player scores for the game (aggregate). */
        private List<EhockeyPlayer> players;
        /** Score breakdown by periods. */
        private List<EhockeyPeriod> periods;
    }

    @Data
    public static class EhockeyPlayer {
        /** Player identifier. */
        private Long id;
        /** Number of goals scored by the player. */
        private Integer goalScore;
    }

    @Data
    public static class EhockeyPeriod {
        /** Index of the period within the game. */
        private Integer index;
        /** Player scores for the period. */
        private List<EhockeyPlayer> players;
    }
}
