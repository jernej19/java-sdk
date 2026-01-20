package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Details about a match within a fixture message.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureMatch {
    /** Match ID */
    private Long id;
    /** Match name (e.g., T1 vs G2) */
    private String name;
    /** Match slug */
    private String slug;
    /** Scheduled time ISO8601 */
    @JsonProperty("scheduled_at") private String scheduledAt;
    /** Original scheduled time if rescheduled ISO8601 */
    @JsonProperty("original_scheduled_at") private String originalScheduledAt;
    /** Actual start time ISO8601 */
    @JsonProperty("begin_at") private String beginAt;
    /** Match end time ISO8601 */
    @JsonProperty("end_at") private String endAt;
    /** Last modified timestamp ISO8601 */
    @JsonProperty("modified_at") private String modifiedAt;
    /** Detailed stats available */
    @JsonProperty("detailed_stats") private Boolean detailedStats;
    /** Betting metadata */
    @JsonProperty("betting_metadata") private FixtureMatchBettingMetadata bettingMetadata;
    /** League ID */
    @JsonProperty("league_id") private Long leagueId;
    /** League information */
    private League league;
    /** Series ID */
    @JsonProperty("serie_id") private Long serieId;
    /** Series information */
    private FixtureSerie serie;
    /** Tournament ID */
    @JsonProperty("tournament_id") private Long tournamentId;
    /** Tournament information */
    private Tournament tournament;
    /** Videogame information */
    private Videogame videogame;
    /** Videogame version */
    @JsonProperty("videogame_version") private VideogameVersion videogameVersion;
    /** Match status */
    private String status;  // Can use MatchStatus enum
    /** Match format type */
    @JsonProperty("match_type") private String matchType;  // Can use MatchType enum
    /** Maximum number of games */
    @JsonProperty("number_of_games") private Integer numberOfGames;
    /** Team ID with game advantage if any */
    @JsonProperty("game_advantage") private Long gameAdvantage;
    /** Whether match ended in a draw */
    private Boolean draw;
    /** Whether match was forfeited */
    private Boolean forfeit;
    /** Whether match was rescheduled */
    private Boolean rescheduled;
    /** Participants */
    private List<FixtureOpponent> opponents;
    /** Winner team/player ID */
    @JsonProperty("winner_id") private Long winnerId;
    /** Winner if decided */
    private FixtureWinner winner;
    /** Match results (scores) */
    private List<MatchResult> results;
    /** Individual games in the match */
    private List<Game> games;
    /** Live streaming information */
    private Live live;
    /** Embeddable live stream URL */
    @JsonProperty("live_embed_url") private String liveEmbedUrl;
    /** Official stream URL */
    @JsonProperty("official_stream_url") private String officialStreamUrl;
    /** Legacy streams object (by language) */
    private Streams streams;
    /** List of all available streams */
    @JsonProperty("streams_list") private List<StreamInfo> streamsList;
}
