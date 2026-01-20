package com.pandascore.sdk.model.feed.markets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import lombok.Data;

/**
 * Selection data for a single market.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketsMessageSelection {
    /** Identifier of the selection. */
    private String id;
    /** Position of the selection within the market. */
    private Integer position;
    /** Display name of the selection. */
    private String name;
    /** Template used for this selection. */
    private String template;
    /** Line associated with the selection. */
    private String line;
    /** Type of participant the selection refers to. */
    @JsonProperty("participant_type") private String participantType;
    /** Identifier of the participant. */
    @JsonProperty("participant_id") private Long participantId;
    /** Opponent type when the selection represents a matchup. */
    @JsonProperty("opponent_type") private String opponentType;
    /** Raw probability of the selection. */
    private Double probability;
    /** Probability adjusted with overround. */
    @JsonProperty("probability_with_overround") private Double probabilityWithOverround;
    /** Decimal odds for the selection. */
    @JsonProperty("odds_decimal") private Double oddsDecimal;
    /** Decimal odds including overround. */
    @JsonProperty("odds_decimal_with_overround") private Double oddsDecimalWithOverround;
    /** Decimal odds including margin. */
    @JsonProperty("odds_decimal_with_margin") private Double oddsDecimalWithMargin;
    /** Probability including margin. */
    @JsonProperty("probability_with_margin") private Double probabilityWithMargin;
    /** Handicap value for spread markets. */
    private Double handicap;
    /** Side of the participant, if relevant. */
    @JsonProperty("participant_side") private String participantSide;
    /** Away team score used when settling the selection. */
    @JsonProperty("score_away") private Integer scoreAway;
    /** Home team score used when settling the selection. */
    @JsonProperty("score_home") private Integer scoreHome;
    /** Number associated with this selection (e.g. map or game). */
    private Integer number;
    /** Identifier of the opposing participant. */
    @JsonProperty("opponent_id") private Long opponentId;
    /** Maximum value for range selections. */
    @JsonProperty("range_max") private Double rangeMax;
    /** Minimum value for range selections. */
    @JsonProperty("range_min") private Double rangeMin;
    /** Settlement result for the selection. */
    private String result;

    /** American odds calculated from the decimal values when enabled. */
    @JsonProperty("odds_american") private Double oddsAmerican;

    /** American odds using the overround decimal value when enabled. */
    @JsonProperty("odds_american_with_overround") private Double oddsAmericanWithOverround;

    /** Fractional odds calculated from the decimal values when enabled. */
    @JsonProperty("odds_fractional") private String oddsFractional;

    /** Fractional odds using the overround decimal value when enabled. */
    @JsonProperty("odds_fractional_with_overround") private String oddsFractionalWithOverround;

    @JsonProperty("odds_decimal")
    public void setOddsDecimal(Double oddsDecimal) {
        this.oddsDecimal = oddsDecimal;
        computeExtraOdds();
    }

    @JsonProperty("odds_decimal_with_overround")
    public void setOddsDecimalWithOverround(Double oddsDecimalWithOverround) {
        this.oddsDecimalWithOverround = oddsDecimalWithOverround;
        computeExtraOdds();
    }

    private void computeExtraOdds() {
        SDKOptions opts;
        try {
            opts = SDKConfig.getInstance().getOptions();
        } catch (Exception e) {
            return; // options not yet initialised
        }
        // reset previously computed values
        oddsAmerican = null;
        oddsAmericanWithOverround = null;
        oddsFractional = null;
        oddsFractionalWithOverround = null;
        if (opts.isAmericanOdds()) {
            if (oddsDecimal != null) {
                oddsAmerican = toAmerican(oddsDecimal);
            }
            if (oddsDecimalWithOverround != null) {
                oddsAmericanWithOverround = toAmerican(oddsDecimalWithOverround);
            }
        }
        if (opts.isFractionalOdds()) {
            if (oddsDecimal != null) {
                oddsFractional = toFractional(oddsDecimal);
            }
            if (oddsDecimalWithOverround != null) {
                oddsFractionalWithOverround = toFractional(oddsDecimalWithOverround);
            }
        }
    }

    private static double toAmerican(double decimal) {
        if (decimal >= 2.0) {
            return Math.round((decimal - 1.0) * 100);
        }
        return Math.round(-100 / (decimal - 1.0));
    }

    private static String toFractional(double decimal) {
        double diff = decimal - 1.0;
        int denom = 100;
        int num = (int) Math.round(diff * denom);
        int g = gcd(Math.abs(num), denom);
        num /= g;
        denom /= g;
        return num + "/" + denom;
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }
}
