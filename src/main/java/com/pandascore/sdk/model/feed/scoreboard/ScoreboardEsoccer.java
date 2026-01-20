package com.pandascore.sdk.model.feed.scoreboard;

import lombok.Data;
import java.time.Instant;
import java.util.List;

/**
 * Scoreboard message for eSoccer matches.
 */
@Data
public class ScoreboardEsoccer {
    /** Identifier of the scoreboard. */
    private long id;
    /** Last update time of this scoreboard. */
    private Instant updatedAt;
    /** List of games that make up the match. */
    private List<EsoccerGame> games;
    /** Type of scoreboard emitted by the feed. */
    private String scoreboardType;

    @Data
    public static class EsoccerGame {
        /** Game identifier. */
        private long id;
        /** Game number within the match. */
        private int position;
        /** Game status. */
        private String status;
        /** Match timer object (counts UP: 0-600 for 2 halves). */
        private EsoccerTimerObject timer;
        /** Current half (1 = first half, 2 = second half). */
        private Integer currentHalf;
        /** Player scores for the game. */
        private List<EsoccerPlayer> players;
        /** Score breakdown by halves. */
        private List<EsoccerHalf> halves;
    }

    @Data
    public static class EsoccerPlayer {
        /** Player identifier. */
        private long id;
        /** Number of goals scored by the player. */
        private int goalScore;
    }

    @Data
    public static class EsoccerHalf {
        /** Index of the half within the game. */
        private int index;
        /** Player scores for the half. */
        private List<EsoccerPlayer> scores;
    }
}
