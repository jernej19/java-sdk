package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Tournament information.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tournament {
    /** Tournament ID */
    private Long id;
    /** Tournament name */
    private String name;
    /** Tournament slug */
    private String slug;
    /** Tournament start time ISO8601 */
    @JsonProperty("begin_at") private String beginAt;
    /** Tournament end time ISO8601 */
    @JsonProperty("end_at") private String endAt;
    /** Parent league ID */
    @JsonProperty("league_id") private Long leagueId;
    /** Parent series ID */
    @JsonProperty("serie_id") private Long serieId;
    /** Tier label (e.g., s, a, b) */
    private String tier;
    /** Tournament type (e.g., online, offline) */
    private String type;
    /** Tournament region */
    private String region;
    /** Tournament country */
    private String country;
    /** Prize pool */
    private String prizepool;
    /** Whether live odds are supported */
    @JsonProperty("live_supported") private Boolean liveSupported;
    /** Winner ID */
    @JsonProperty("winner_id") private Long winnerId;
    /** Winner type ("Team" | "Player") */
    @JsonProperty("winner_type") private String winnerType;
    /** Last modified timestamp */
    @JsonProperty("modified_at") private String modifiedAt;
}
