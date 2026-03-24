package com.pandascore.sdk.rmq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.EventHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RabbitMQFeed's recovery buffering logic (startRecovery/endRecovery).
 * These tests exercise the recovery buffer without requiring an actual AMQP broker.
 */
class RabbitMQFeedRecoveryBufferTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private EventHandler handler;
    private RabbitMQFeed feed;

    @BeforeEach
    void setUp() {
        SDKConfig.setOptions(SDKOptions.builder()
            .apiToken("token")
            .companyId(1)
            .email("e@e.com")
            .password("pass")
            .apiBaseUrl("http://localhost:9999")
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q").routingKey("r").build())
            .build());

        handler = new EventHandler(event -> {});
        feed = new RabbitMQFeed(handler);
        RabbitMQFeed.resetActiveConnectionCount();
    }

    @AfterEach
    void tearDown() {
        handler.close();
        RabbitMQFeed.resetActiveConnectionCount();
    }

    // --- Helpers to access private fields ---

    private boolean isRecovering() throws Exception {
        Field f = RabbitMQFeed.class.getDeclaredField("recovering");
        f.setAccessible(true);
        return (boolean) f.get(feed);
    }

    @SuppressWarnings("unchecked")
    private Queue<JsonNode> getRecoveryBuffer() throws Exception {
        Field f = RabbitMQFeed.class.getDeclaredField("recoveryBuffer");
        f.setAccessible(true);
        return (Queue<JsonNode>) f.get(feed);
    }

    private void setCustomerSink(Consumer<Object> sink) throws Exception {
        Field f = RabbitMQFeed.class.getDeclaredField("customerSink");
        f.setAccessible(true);
        f.set(feed, sink);
    }

    // ============================================================
    //  startRecovery
    // ============================================================

    @Test
    @DisplayName("startRecovery sets recovering flag to true")
    void startRecovery_setsFlag() throws Exception {
        assertFalse(isRecovering());
        feed.startRecovery();
        assertTrue(isRecovering());
    }

    @Test
    @DisplayName("startRecovery clears existing buffer")
    void startRecovery_clearsBuffer() throws Exception {
        Queue<JsonNode> buffer = getRecoveryBuffer();
        buffer.add(mapper.readTree("{\"stale\": true}"));
        assertEquals(1, buffer.size());

        feed.startRecovery();
        assertTrue(buffer.isEmpty());
    }

    // ============================================================
    //  endRecovery
    // ============================================================

    @Test
    @DisplayName("endRecovery sets recovering flag to false")
    void endRecovery_clearsFlag() throws Exception {
        feed.startRecovery();
        assertTrue(isRecovering());
        feed.endRecovery();
        assertFalse(isRecovering());
    }

    @Test
    @DisplayName("endRecovery drains buffered messages to sink")
    void endRecovery_drainsBuffer() throws Exception {
        CopyOnWriteArrayList<Object> received = new CopyOnWriteArrayList<>();
        setCustomerSink(received::add);

        // Start recovery and add messages to buffer
        feed.startRecovery();
        Queue<JsonNode> buffer = getRecoveryBuffer();
        buffer.add(mapper.readTree("{\"type\": \"markets\", \"id\": 1}"));
        buffer.add(mapper.readTree("{\"type\": \"fixture\", \"id\": 2}"));
        buffer.add(mapper.readTree("{\"type\": \"scoreboard\", \"id\": 3}"));

        feed.endRecovery();

        assertEquals(3, received.size());
        assertFalse(isRecovering());
        assertTrue(buffer.isEmpty());
    }

    @Test
    @DisplayName("endRecovery with empty buffer processes nothing")
    void endRecovery_emptyBuffer() throws Exception {
        CopyOnWriteArrayList<Object> received = new CopyOnWriteArrayList<>();
        setCustomerSink(received::add);

        feed.startRecovery();
        feed.endRecovery();

        assertTrue(received.isEmpty());
        assertFalse(isRecovering());
    }

    @Test
    @DisplayName("endRecovery with null sink does not throw")
    void endRecovery_nullSink_noException() throws Exception {
        setCustomerSink(null);

        feed.startRecovery();
        Queue<JsonNode> buffer = getRecoveryBuffer();
        buffer.add(mapper.readTree("{\"type\": \"markets\"}"));

        assertDoesNotThrow(() -> feed.endRecovery());
    }

    @Test
    @DisplayName("endRecovery handles sink exception gracefully")
    void endRecovery_sinkException_handledGracefully() throws Exception {
        Consumer<Object> failingSink = msg -> {
            throw new RuntimeException("sink error");
        };
        setCustomerSink(failingSink);

        feed.startRecovery();
        Queue<JsonNode> buffer = getRecoveryBuffer();
        buffer.add(mapper.readTree("{\"type\": \"markets\"}"));
        buffer.add(mapper.readTree("{\"type\": \"fixture\"}"));

        // Should not throw despite sink failure
        assertDoesNotThrow(() -> feed.endRecovery());
        assertFalse(isRecovering());
    }

    // ============================================================
    //  Recovery lifecycle
    // ============================================================

    @Test
    @DisplayName("Full recovery lifecycle: start → buffer → end → drain")
    void fullRecoveryLifecycle() throws Exception {
        CopyOnWriteArrayList<Object> received = new CopyOnWriteArrayList<>();
        setCustomerSink(received::add);

        // Phase 1: Normal — no recovery
        assertFalse(isRecovering());

        // Phase 2: Start recovery
        feed.startRecovery();
        assertTrue(isRecovering());

        // Phase 3: Messages arrive during recovery — buffered
        Queue<JsonNode> buffer = getRecoveryBuffer();
        buffer.add(mapper.readTree("{\"type\": \"markets\", \"seq\": 1}"));
        buffer.add(mapper.readTree("{\"type\": \"markets\", \"seq\": 2}"));

        // Sink should NOT have received anything yet
        assertTrue(received.isEmpty());

        // Phase 4: End recovery — buffer drains
        feed.endRecovery();
        assertFalse(isRecovering());
        assertEquals(2, received.size());
    }

    @Test
    @DisplayName("Multiple recovery cycles work correctly")
    void multipleRecoveryCycles() throws Exception {
        CopyOnWriteArrayList<Object> received = new CopyOnWriteArrayList<>();
        setCustomerSink(received::add);

        // Cycle 1
        feed.startRecovery();
        getRecoveryBuffer().add(mapper.readTree("{\"cycle\": 1}"));
        feed.endRecovery();
        assertEquals(1, received.size());

        // Cycle 2
        feed.startRecovery();
        getRecoveryBuffer().add(mapper.readTree("{\"cycle\": 2}"));
        getRecoveryBuffer().add(mapper.readTree("{\"cycle\": 2}"));
        feed.endRecovery();
        assertEquals(3, received.size());
    }
}
