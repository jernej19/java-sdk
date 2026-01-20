package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Player information.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Player {
    /** Player ID */
    private Long id;
    /** Player name */
    private String name;
    /** Player slug */
    private String slug;
    /** Player role (nullable) */
    private String role;
}
