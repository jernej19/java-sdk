package com.pandascore.sdk.model.feed.markets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MarketAction enum deserialization.
 */
class MarketActionTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("All MarketAction enum values exist")
    void allValuesExist() {
        MarketAction[] values = MarketAction.values();
        assertEquals(9, values.length);
        assertNotNull(MarketAction.valueOf("created"));
        assertNotNull(MarketAction.valueOf("margin_changed"));
        assertNotNull(MarketAction.valueOf("odds_changed"));
        assertNotNull(MarketAction.valueOf("suspended"));
        assertNotNull(MarketAction.valueOf("deactivated"));
        assertNotNull(MarketAction.valueOf("settled"));
        assertNotNull(MarketAction.valueOf("rollback_settlement"));
        assertNotNull(MarketAction.valueOf("opponent_updated"));
        assertNotNull(MarketAction.valueOf("partially_settled"));
    }

    @Test
    @DisplayName("Deserialize each MarketAction via JSON")
    void deserialize_eachAction() throws Exception {
        for (MarketAction action : MarketAction.values()) {
            String json = "{\"action\": \"" + action.name() + "\"}";
            MarketsMessage msg = mapper.readValue(json, MarketsMessage.class);
            assertEquals(action, msg.getAction());
        }
    }

    @Test
    @DisplayName("Null action deserializes to null")
    void deserialize_nullAction() throws Exception {
        String json = "{\"type\": \"markets\"}";
        MarketsMessage msg = mapper.readValue(json, MarketsMessage.class);
        assertNull(msg.getAction());
    }
}
