package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Winner information included in a fixture message.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureWinner {
    /** Winner team ID */
    private Long id;
    /** Winner type ("Team" | "Player") */
    private String type;
    /** Winner acronym */
    private String acronym;
    /** Winner image/logo */
    @JsonProperty("image_url") private String imageUrl;
    /** Dark mode image/logo */
    @JsonProperty("dark_mode_image_url") private String darkModeImageUrl;
    /** Winner location */
    private String location;
    /** Last update timestamp */
    @JsonProperty("modified_at") private String modifiedAt;
    /** Winner name */
    private String name;
    /** Winner slug */
    private String slug;
}
