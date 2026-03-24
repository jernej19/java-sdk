package com.pandascore.sdk.model.feed.markets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MarketsMessageSelection — odds conversion logic.
 */
class MarketsMessageSelectionTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private void configureSDK(boolean american, boolean fractional) {
        SDKConfig.setOptions(SDKOptions.builder()
            .apiToken("t")
            .companyId(1)
            .email("e@e.com")
            .password("p")
            .americanOdds(american)
            .fractionalOdds(fractional)
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q").routingKey("r").build())
            .build());
    }

    // ============================================================
    //  Basic deserialization
    // ============================================================

    @Test
    @DisplayName("Deserialize selection with all basic fields")
    void deserialize_basicFields() throws Exception {
        configureSDK(false, false);
        String json = """
            {
              "id": "sel-1",
              "position": 1,
              "name": "Home Win",
              "template": "match_winner",
              "line": "0",
              "participant_type": "Team",
              "participant_id": 100,
              "probability": 0.65,
              "probability_with_overround": 0.68,
              "odds_decimal": 1.54,
              "odds_decimal_with_overround": 1.47,
              "odds_decimal_with_margin": 1.50,
              "probability_with_margin": 0.67,
              "handicap": -1.5,
              "participant_side": "home",
              "score_away": 0,
              "score_home": 2,
              "result": "win"
            }
            """;

        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertEquals("sel-1", sel.getId());
        assertEquals(1, sel.getPosition());
        assertEquals("Home Win", sel.getName());
        assertEquals("match_winner", sel.getTemplate());
        assertEquals("0", sel.getLine());
        assertEquals("Team", sel.getParticipantType());
        assertEquals(100L, sel.getParticipantId());
        assertEquals(0.65, sel.getProbability(), 0.001);
        assertEquals(0.68, sel.getProbabilityWithOverround(), 0.001);
        assertEquals(1.54, sel.getOddsDecimal(), 0.001);
        assertEquals(1.47, sel.getOddsDecimalWithOverround(), 0.001);
        assertEquals(1.50, sel.getOddsDecimalWithMargin(), 0.001);
        assertEquals(0.67, sel.getProbabilityWithMargin(), 0.001);
        assertEquals(-1.5, sel.getHandicap(), 0.001);
        assertEquals("home", sel.getParticipantSide());
        assertEquals(0, sel.getScoreAway());
        assertEquals(2, sel.getScoreHome());
        assertEquals("win", sel.getResult());
    }

    // ============================================================
    //  American odds computation
    // ============================================================

    @Test
    @DisplayName("American odds computed when enabled — favorite (decimal < 2.0)")
    void americanOdds_favorite() throws Exception {
        configureSDK(true, false);
        String json = "{\"odds_decimal\": 1.5}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertNotNull(sel.getOddsAmerican());
        // 1.5 → -100 / (1.5 - 1.0) = -200
        assertEquals(-200.0, sel.getOddsAmerican(), 1.0);
    }

    @Test
    @DisplayName("American odds computed when enabled — underdog (decimal >= 2.0)")
    void americanOdds_underdog() throws Exception {
        configureSDK(true, false);
        String json = "{\"odds_decimal\": 3.0}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertNotNull(sel.getOddsAmerican());
        // 3.0 → (3.0 - 1.0) * 100 = +200
        assertEquals(200.0, sel.getOddsAmerican(), 1.0);
    }

    @Test
    @DisplayName("American odds for even money (decimal = 2.0)")
    void americanOdds_evenMoney() throws Exception {
        configureSDK(true, false);
        String json = "{\"odds_decimal\": 2.0}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertNotNull(sel.getOddsAmerican());
        // 2.0 → (2.0 - 1.0) * 100 = +100
        assertEquals(100.0, sel.getOddsAmerican(), 1.0);
    }

    @Test
    @DisplayName("American odds for overround decimal is also computed")
    void americanOdds_withOverround() throws Exception {
        configureSDK(true, false);
        String json = "{\"odds_decimal\": 2.5, \"odds_decimal_with_overround\": 2.3}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertNotNull(sel.getOddsAmerican());
        assertNotNull(sel.getOddsAmericanWithOverround());
    }

    @Test
    @DisplayName("American odds are null when disabled")
    void americanOdds_disabled() throws Exception {
        configureSDK(false, false);
        String json = "{\"odds_decimal\": 2.5}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertNull(sel.getOddsAmerican());
        assertNull(sel.getOddsAmericanWithOverround());
    }

    // ============================================================
    //  Fractional odds computation
    // ============================================================

    @Test
    @DisplayName("Fractional odds computed when enabled")
    void fractionalOdds_computed() throws Exception {
        configureSDK(false, true);
        String json = "{\"odds_decimal\": 2.5}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertNotNull(sel.getOddsFractional());
        // 2.5 → diff = 1.5 → num = 150, denom = 100 → gcd(150,100) = 50 → 3/2
        assertEquals("3/2", sel.getOddsFractional());
    }

    @Test
    @DisplayName("Fractional odds for even money (2.0 → 1/1)")
    void fractionalOdds_evenMoney() throws Exception {
        configureSDK(false, true);
        String json = "{\"odds_decimal\": 2.0}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertEquals("1/1", sel.getOddsFractional());
    }

    @Test
    @DisplayName("Fractional odds for favorite (1.5 → 1/2)")
    void fractionalOdds_favorite() throws Exception {
        configureSDK(false, true);
        String json = "{\"odds_decimal\": 1.5}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertEquals("1/2", sel.getOddsFractional());
    }

    @Test
    @DisplayName("Fractional odds with overround are also computed")
    void fractionalOdds_withOverround() throws Exception {
        configureSDK(false, true);
        String json = "{\"odds_decimal\": 3.0, \"odds_decimal_with_overround\": 2.8}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertNotNull(sel.getOddsFractional());
        assertNotNull(sel.getOddsFractionalWithOverround());
    }

    @Test
    @DisplayName("Fractional odds are null when disabled")
    void fractionalOdds_disabled() throws Exception {
        configureSDK(false, false);
        String json = "{\"odds_decimal\": 2.5}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertNull(sel.getOddsFractional());
        assertNull(sel.getOddsFractionalWithOverround());
    }

    // ============================================================
    //  Both American and Fractional enabled
    // ============================================================

    @Test
    @DisplayName("Both American and Fractional odds computed when both enabled")
    void bothOddsEnabled() throws Exception {
        configureSDK(true, true);
        String json = "{\"odds_decimal\": 3.0, \"odds_decimal_with_overround\": 2.8}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertNotNull(sel.getOddsAmerican());
        assertNotNull(sel.getOddsAmericanWithOverround());
        assertNotNull(sel.getOddsFractional());
        assertNotNull(sel.getOddsFractionalWithOverround());

        assertEquals(200.0, sel.getOddsAmerican(), 1.0);
        assertEquals("2/1", sel.getOddsFractional());
    }

    // ============================================================
    //  Null odds
    // ============================================================

    @Test
    @DisplayName("Null odds_decimal does not cause errors")
    void nullOddsDecimal_noError() throws Exception {
        configureSDK(true, true);
        String json = "{\"id\": \"sel-1\"}";
        MarketsMessageSelection sel = mapper.readValue(json, MarketsMessageSelection.class);

        assertNull(sel.getOddsDecimal());
        assertNull(sel.getOddsAmerican());
        assertNull(sel.getOddsFractional());
    }
}
