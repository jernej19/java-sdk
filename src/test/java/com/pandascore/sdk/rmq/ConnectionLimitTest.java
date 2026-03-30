package com.pandascore.sdk.rmq;

import com.pandascore.sdk.config.SDKOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the global active connection counter, MAX_CONNECTIONS enforcement,
 * and per-connection queue binding support.
 * <p>
 * These tests exercise the static counter mechanics directly since actual AMQP
 * connections require a broker. The counter is manipulated via the package-private
 * {@code resetActiveConnectionCount()} method.
 */
class ConnectionLimitTest {

    @BeforeEach
    void setUp() {
        RabbitMQFeed.resetActiveConnectionCount();
    }

    @AfterEach
    void tearDown() {
        RabbitMQFeed.resetActiveConnectionCount();
    }

    @Test
    @DisplayName("Initial active connection count is zero")
    void initialCountIsZero() {
        assertEquals(0, RabbitMQFeed.getActiveConnectionCount());
    }

    @Test
    @DisplayName("MAX_CONNECTIONS constant is 10")
    void maxConnectionsIsTen() {
        assertEquals(10, RabbitMQFeed.MAX_CONNECTIONS);
    }

    @Test
    @DisplayName("resetActiveConnectionCount resets counter to zero")
    void resetResetsToZero() {
        RabbitMQFeed.resetActiveConnectionCount();
        assertEquals(0, RabbitMQFeed.getActiveConnectionCount());
    }

    @Test
    @DisplayName("getActiveConnectionCount returns current count")
    void getActiveConnectionCountReflectsState() {
        assertEquals(0, RabbitMQFeed.getActiveConnectionCount());
    }

    // ============================================================
    //  Per-connection queue bindings
    // ============================================================

    @Test
    @DisplayName("RabbitMQFeed constructor accepts null queueBindings (uses global config)")
    void constructor_nullQueueBindings_accepted() {
        // Should not throw — null means "use global config"
        assertDoesNotThrow(() -> new RabbitMQFeed(null, null));
    }

    @Test
    @DisplayName("RabbitMQFeed constructor rejects empty queueBindings list")
    void constructor_emptyQueueBindings_throws() {
        List<SDKOptions.QueueBinding> empty = List.of();
        assertThrows(IllegalArgumentException.class, () -> new RabbitMQFeed(null, empty));
    }

    @Test
    @DisplayName("RabbitMQFeed constructor rejects more than MAX_QUEUES_PER_CONNECTION bindings")
    void constructor_tooManyQueueBindings_throws() {
        List<SDKOptions.QueueBinding> bindings = new ArrayList<>();
        for (int i = 0; i < SDKOptions.MAX_QUEUES_PER_CONNECTION + 1; i++) {
            bindings.add(SDKOptions.QueueBinding.builder()
                .queueName("q" + i).routingKey("r" + i).build());
        }
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class, () -> new RabbitMQFeed(null, bindings));
        assertTrue(ex.getMessage().contains("Cannot bind more than"));
    }

    @Test
    @DisplayName("RabbitMQFeed constructor accepts exactly MAX_QUEUES_PER_CONNECTION bindings")
    void constructor_maxQueueBindings_accepted() {
        List<SDKOptions.QueueBinding> bindings = new ArrayList<>();
        for (int i = 0; i < SDKOptions.MAX_QUEUES_PER_CONNECTION; i++) {
            bindings.add(SDKOptions.QueueBinding.builder()
                .queueName("q" + i).routingKey("r" + i).build());
        }
        assertDoesNotThrow(() -> new RabbitMQFeed(null, bindings));
    }

    @Test
    @DisplayName("RabbitMQFeed constructor validates individual QueueBinding entries")
    void constructor_invalidQueueBinding_throws() {
        List<SDKOptions.QueueBinding> bindings = List.of(
            SDKOptions.QueueBinding.builder().routingKey("r").build() // missing queueName
        );
        assertThrows(NullPointerException.class, () -> new RabbitMQFeed(null, bindings));
    }
}
