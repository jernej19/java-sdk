package com.pandascore.sdk.model.feed.scoreboard;

import lombok.Data;
import java.time.Instant;
import java.util.List;

/**
 * Scoreboard message for eTennis matches.
 */
@Data
public class ScoreboardEtennis {
    /** PandaScore match identifier. */
    private long id;
    /** Timestamp at which this scoreboard was last updated. */
    private Instant updatedAt;
    /** Sets that compose the eTennis match. */
    private List<EtennisSet> sets;

    /**
     * A single set within an eTennis match.
     */
    @Data
    public static class EtennisSet {
        /** PandaScore eTennis set identifier. */
        private long id;
        /** Position of the set within the match. */
        private int position;
        /** Current status of the set. */
        private String status;
        /** Index of the game currently being played within the set. */
        private Integer currentGame;
        /** Player scores for the set. */
        private List<EtennisSetPlayer> players;
        /** Games within the set. */
        private List<EtennisGame> games;
    }

    /**
     * Player score within a set.
     */
    @Data
    public static class EtennisSetPlayer {
        /** PandaScore player identifier. */
        private long id;
        /** Number of games won by the player in the current set. */
        private int setScore;
    }

    /**
     * A single game within an eTennis set.
     */
    @Data
    public static class EtennisGame {
        /** Position of the game within the set. */
        private int index;
        /** Identifier of the player currently serving. */
        private Integer server;
        /** Player scores for the game. */
        private List<EtennisGamePlayer> players;
    }

    /**
     * Player score within a game. The {@code gameScore} field can be an integer
     * (0, 15, 30, 40) or a string ({@code "adv"}).
     */
    @Data
    public static class EtennisGamePlayer {
        /** PandaScore player identifier. */
        private long id;
        /** Current score in the game (0, 15, 30, 40, or "adv"). */
        private Object gameScore;
    }
}
