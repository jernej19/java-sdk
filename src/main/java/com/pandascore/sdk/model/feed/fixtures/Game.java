package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Individual game/map within a match.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Game {
    /** Game ID */
    private Long id;
    /** Game position in match */
    private Integer position;
    /** Parent match ID */
    @JsonProperty("match_id") private Long matchId;
    /** Game status */
    private String status;  // Can use GameStatus enum
    /** Game start time ISO8601 */
    @JsonProperty("begin_at") private String beginAt;
    /** Game end time ISO8601 */
    @JsonProperty("end_at") private String endAt;
    /** Whether game is finished */
    private Boolean finished;
    /** Whether game data is complete */
    private Boolean complete;
    /** Whether detailed stats are available */
    @JsonProperty("detailed_stats") private Boolean detailedStats;
    /** Whether game ended in a draw */
    private Boolean draw;
    /** Whether game was forfeited */
    private Boolean forfeit;
    /** Game duration in seconds */
    private Long length;
    /** Map information */
    private GameMap map;
    /** Number of rounds played */
    @JsonProperty("number_of_rounds") private Integer numberOfRounds;
    /** Round scores by team */
    @JsonProperty("rounds_score") private List<RoundScore> roundsScore;
    /** VOD URL */
    @JsonProperty("video_url") private String videoUrl;
    /** Game winner */
    private GameWinner winner;
    /** Winner type ("Team" | "Player") */
    @JsonProperty("winner_type") private String winnerType;
    /** Detailed round-by-round data (optional, can be large) */
    @JsonProperty("game_round_teams") private List<GameRoundTeam> gameRoundTeams;
}
