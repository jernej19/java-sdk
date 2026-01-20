package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Detailed stream information.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamInfo {
    /** Embeddable stream URL */
    @JsonProperty("embed_url") private String embedUrl;
    /** Stream language code */
    private String language;
    /** Whether this is the main stream */
    private Boolean main;
    /** Whether this is an official stream */
    private Boolean official;
    /** Direct stream URL */
    @JsonProperty("raw_url") private String rawUrl;
}
