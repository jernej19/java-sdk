package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Blueprint information included in fixture messages.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Blueprint {
    private Long id;
    private String name;
}
