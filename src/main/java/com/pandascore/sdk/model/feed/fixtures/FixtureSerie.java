package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Series information contained in a fixture message.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureSerie {
    private Long id;
    @JsonProperty("begin_at") private String beginAt;
    private String description;
    @JsonProperty("end_at") private String endAt;
    @JsonProperty("full_name") private String fullName;
    @JsonProperty("league_id") private Long leagueId;
    @JsonProperty("league_image_url") private String leagueImageUrl;
    @JsonProperty("league_name") private String leagueName;
    @JsonProperty("modified_at") private String modifiedAt;
    private String name;
    private String season;
    private String slug;
    private String tier;
    @JsonProperty("videogame_title") private VideogameTitle videogameTitle;
    @JsonProperty("winner_id") private Long winnerId;
    @JsonProperty("winner_type") private String winnerType;
    private Integer year;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideogameTitle {
        private Long id;
        private String name;
        private String slug;
    }
}
