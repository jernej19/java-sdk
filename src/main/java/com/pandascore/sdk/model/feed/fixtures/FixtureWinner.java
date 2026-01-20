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
    private Long id;
    private String type;
    private String acronym;
    @JsonProperty("image_url") private String imageUrl;
    private String location;
    @JsonProperty("modified_at") private String modifiedAt;
    private String name;
    private String slug;
}
