package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Basic team information for a fixture.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureTeam {
    private Long id;
    private String name;
    private String slug;
    private String acronym;
    @JsonProperty("image_url") private String imageUrl;
    private String location;
}
