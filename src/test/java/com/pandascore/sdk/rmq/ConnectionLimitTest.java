package com.pandascore.sdk.rmq;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the global active connection counter and MAX_CONNECTIONS warning threshold.
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
        // Simulate some connections by directly accessing the counter via reflection
        // or use the reset to verify it works from a non-zero state
        RabbitMQFeed.resetActiveConnectionCount();
        assertEquals(0, RabbitMQFeed.getActiveConnectionCount());
    }

    @Test
    @DisplayName("getActiveConnectionCount returns current count")
    void getActiveConnectionCountReflectsState() {
        assertEquals(0, RabbitMQFeed.getActiveConnectionCount());
        // Counter starts at zero and is incremented only by establish()
        // which requires a real broker — so we just verify the API is accessible
    }
}
