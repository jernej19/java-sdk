package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Map information for a game.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameMap {
    /** Map ID */
    private Long id;
    /** Map name */
    private String name;
    /** Map slug */
    private String slug;
    /** Map image URL */
    private String image;
    /** Game mode */
    @JsonProperty("game_mode") private String gameMode;
}
