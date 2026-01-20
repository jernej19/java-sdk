package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Live streaming information.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Live {
    /** When live stream opens ISO8601 */
    @JsonProperty("opens_at") private String opensAt;
    /** Whether live streaming is supported */
    private Boolean supported;
    /** Live stream WebSocket URL */
    private String url;
}
