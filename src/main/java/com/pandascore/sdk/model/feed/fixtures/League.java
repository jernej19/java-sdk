package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * League information.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class League {
    /** League ID */
    private Long id;
    /** League name */
    private String name;
    /** League slug */
    private String slug;
    /** League logo URL */
    @JsonProperty("image_url") private String imageUrl;
    /** League website URL */
    private String url;
    /** Last modified timestamp */
    @JsonProperty("modified_at") private String modifiedAt;
}
