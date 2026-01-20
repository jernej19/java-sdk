package com.pandascore.sdk.model.feed.scoreboard;

import lombok.Data;
import java.time.Instant;
import java.util.List;

/**
 * Scoreboard message for eBasketball matches.
 */
@Data
public class ScoreboardEbasketball {
    /** Identifier of the scoreboard message. */
    private long id;
    /** Moment this scoreboard was updated. */
    private Instant updatedAt;
    /** Games that compose the eBasketball match. */
    private List<EbasketballGame> games;
    /** Type of the scoreboard. */
    private String scoreboardType;

    @Data
    public static class EbasketballGame {
        /** Game identifier. */
        private long id;
        /** Order of the game within the match. */
        private int position;
        /** Status of the game. */
        private String status;
        /** Match timer object (counts DOWN: 300→0 each quarter). */
        private EbasketballTimerObject timer;
        /** Current quarter (1-4 regulation, >4 OT). */
        private Integer currentQuarter;
        /** Individual scores for each player. */
        private List<EbasketballPlayer> players;
        /** Score for each quarter of the game. */
        private List<EbasketballQuarter> quarters;
    }

    @Data
    public static class EbasketballPlayer {
        /** Player identifier. */
        private long id;
        /** Points scored by the player. */
        private int pointScore;
    }

    @Data
    public static class EbasketballQuarter {
        /** Quarter index within the game. */
        private int index;
        /** Player scores for the quarter. */
        private List<EbasketballPlayer> scores;
    }
}
