package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Videogame information.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Videogame {
    /** Videogame ID */
    private Long id;
    /** Videogame name */
    private String name;
    /** Videogame slug */
    private String slug;
}
