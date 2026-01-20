package com.pandascore.sdk.model.feed.scoreboard;

import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Scoreboard message for Valorant matches.
 */
@Data
public class ScoreboardValorant {
    /** Identifier of the scoreboard message. */
    private long id;
    /** Time of the last update. */
    private Instant updatedAt;
    /** Games contained in this scoreboard. */
    private List<ValorantGame> games;
    /** Original type value returned by the feed. */
    private String scoreboardType;

    @Data
    public static class ValorantGame {
        /** Game identifier. */
        private long id;
        /** Position of the game within the series. */
        private int position;
        /** Status of the game. */
        private String status;
        /** Map information if known. */
        private Optional<ValorantMap> map;
        /** Teams participating in the game. */
        private List<ValorantTeam> teams;
    }

    @Data
    public static class ValorantMap {
        /** Map slug. */
        private String slug;
        /** Human friendly map name. */
        private String name;
    }

    @Data
    public static class ValorantTeam {
        /** Identifier of the team. */
        private long id;
        /** Current round score for the team. */
        private int roundScore;
        /** Side of the map being played. */
        private String side;
    }
}
