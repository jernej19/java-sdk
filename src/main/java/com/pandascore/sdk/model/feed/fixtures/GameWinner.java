package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Game-level winner information.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameWinner {
    /** Winner ID */
    private Long id;
    /** Winner type ("Team" | "Player") */
    private String type;
}
