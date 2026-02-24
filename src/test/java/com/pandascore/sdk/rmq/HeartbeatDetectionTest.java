package com.pandascore.sdk.rmq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the heartbeat detection logic extracted from RabbitMQFeed.
 * <p>
 * TypeScript reference: heartbeat messages have an "at" field and NO "type" field.
 * All other messages have a "type" field.
 */
class HeartbeatDetectionTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String jsonStr) throws Exception {
        return mapper.readTree(jsonStr);
    }

    // ============================================================
    //  Messages that ARE heartbeats
    // ============================================================

    @Test
    @DisplayName("Message with 'at' and no 'type' is a heartbeat")
    void heartbeat_atField_noType() throws Exception {
        JsonNode msg = json("{\"at\": \"2025-05-22T14:00:00Z\"}");
        assertTrue(RabbitMQFeed.isHeartbeatMessage(msg));
    }

    @Test
    @DisplayName("Heartbeat with additional fields (but no 'type') is still a heartbeat")
    void heartbeat_atFieldWithExtras_noType() throws Exception {
        JsonNode msg = json("{\"at\": \"2025-05-22T14:00:00Z\", \"seq\": 42}");
        assertTrue(RabbitMQFeed.isHeartbeatMessage(msg));
    }

    @Test
    @DisplayName("Heartbeat with numeric 'at' value is a heartbeat")
    void heartbeat_numericAt_noType() throws Exception {
        JsonNode msg = json("{\"at\": 1716386400}");
        assertTrue(RabbitMQFeed.isHeartbeatMessage(msg));
    }

    // ============================================================
    //  Messages that are NOT heartbeats
    // ============================================================

    @Test
    @DisplayName("Message with 'type' field is NOT a heartbeat (even with 'at')")
    void notHeartbeat_hasTypeAndAt() throws Exception {
        JsonNode msg = json("{\"type\": \"markets\", \"at\": \"2025-05-22T14:00:00Z\"}");
        assertFalse(RabbitMQFeed.isHeartbeatMessage(msg));
    }

    @Test
    @DisplayName("Message with 'type' but no 'at' is NOT a heartbeat")
    void notHeartbeat_hasTypeNoAt() throws Exception {
        JsonNode msg = json("{\"type\": \"fixture\", \"data\": {}}");
        assertFalse(RabbitMQFeed.isHeartbeatMessage(msg));
    }

    @Test
    @DisplayName("Message with neither 'at' nor 'type' is NOT a heartbeat")
    void notHeartbeat_neitherAtNorType() throws Exception {
        JsonNode msg = json("{\"data\": \"something\"}");
        assertFalse(RabbitMQFeed.isHeartbeatMessage(msg));
    }

    @Test
    @DisplayName("Empty JSON object is NOT a heartbeat")
    void notHeartbeat_emptyObject() throws Exception {
        JsonNode msg = json("{}");
        assertFalse(RabbitMQFeed.isHeartbeatMessage(msg));
    }

    @Test
    @DisplayName("Message with type='v1.beat' is NOT treated as heartbeat if it has 'type' field")
    void notHeartbeat_v1BeatType() throws Exception {
        // This is the OLD detection pattern — type=v1.beat WITH a type field
        // should NOT be treated as heartbeat under the new convention
        JsonNode msg = json("{\"type\": \"v1.beat\", \"at\": \"2025-05-22T14:00:00Z\"}");
        assertFalse(RabbitMQFeed.isHeartbeatMessage(msg));
    }

    @Test
    @DisplayName("Scoreboard message is NOT a heartbeat")
    void notHeartbeat_scoreboard() throws Exception {
        JsonNode msg = json("{\"type\": \"scoreboard\", \"at\": \"2025-05-22T14:00:00Z\", \"data\": {}}");
        assertFalse(RabbitMQFeed.isHeartbeatMessage(msg));
    }

    @Test
    @DisplayName("Markets message is NOT a heartbeat")
    void notHeartbeat_markets() throws Exception {
        JsonNode msg = json("{\"type\": \"markets\", \"data\": {\"odds\": []}}");
        assertFalse(RabbitMQFeed.isHeartbeatMessage(msg));
    }
}
