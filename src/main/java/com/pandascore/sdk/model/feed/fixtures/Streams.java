package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Legacy streams object by language.
 * Map with dynamic keys like "english", "russian", "official".
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Streams {
    /** Map of language to stream data */
    private Map<String, StreamData> streams = new HashMap<>();

    @JsonAnySetter
    public void setStream(String language, StreamData streamData) {
        streams.put(language, streamData);
    }

    /**
     * Stream data for a specific language.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StreamData {
        /** Embeddable stream URL */
        @JsonProperty("embed_url") private String embedUrl;
        /** Direct stream URL */
        @JsonProperty("raw_url") private String rawUrl;
    }
}
