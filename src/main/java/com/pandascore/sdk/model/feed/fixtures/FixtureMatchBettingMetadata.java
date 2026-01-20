package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Additional betting information associated with a fixture match.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureMatchBettingMetadata {
    @JsonProperty("betbuilder_enabled") private boolean betbuilderEnabled;
    @JsonProperty("betting_group") private BettingGroup bettingGroup;
    private Blueprint blueprint;
    private boolean bookable;
    private boolean booked;
    @JsonProperty("booked_at") private String bookedAt;
    @JsonProperty("booked_by_user_id") private Long bookedByUserId;
    private String coverage;
    @JsonProperty("inputs_enable") private boolean inputsEnable;
    @JsonProperty("live_available") private boolean liveAvailable;
    @JsonProperty("markets_created") private boolean marketsCreated;
    @JsonProperty("markets_updated_at") private String marketsUpdatedAt;
    @JsonProperty("micromarkets_enabled") private boolean micromarketsEnabled;
    @JsonProperty("pandascore_reviewed") private boolean pandascoreReviewed;
    private boolean settled;
}
