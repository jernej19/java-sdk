package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Videogame version information.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideogameVersion {
    /** Version name */
    private String name;
    /** Whether this is the current version */
    private Boolean current;
}
