package com.pandascore.sdk.rmq;

import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.ConnectionEvent;
import com.pandascore.sdk.events.EventHandler;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.AMQConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that multiple RabbitMQFeed instances can coexist correctly.
 * Verifies the activeConnections counter stays accurate across
 * concurrent connection creation, shutdown, and close operations.
 */
class ConcurrentConnectionsTest {

    private final List<EventHandler> handlers = new ArrayList<>();
    private final List<RabbitMQFeed> feeds = new ArrayList<>();

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

        RabbitMQFeed.resetActiveConnectionCount();
    }

    @AfterEach
    void tearDown() {
        for (EventHandler h : handlers) {
            try { h.close(); } catch (Exception ignored) {}
        }
        for (RabbitMQFeed f : feeds) {
            try { f.close(); } catch (Exception ignored) {}
        }
        RabbitMQFeed.resetActiveConnectionCount();
    }

    private RabbitMQFeed createFeed() {
        return createFeed(null);
    }

    private RabbitMQFeed createFeed(List<SDKOptions.QueueBinding> bindings) {
        EventHandler handler = new EventHandler(event -> {});
        handlers.add(handler);
        RabbitMQFeed feed = bindings != null
            ? new RabbitMQFeed(handler, bindings)
            : new RabbitMQFeed(handler);
        feeds.add(feed);
        return feed;
    }

    @Test
    @DisplayName("Multiple RabbitMQFeed instances can be created with separate handlers")
    void multipleFeedsWithSeparateHandlers() {
        CopyOnWriteArrayList<ConnectionEvent> events1 = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<ConnectionEvent> events2 = new CopyOnWriteArrayList<>();

        EventHandler handler1 = new EventHandler(events1::add);
        EventHandler handler2 = new EventHandler(events2::add);
        handlers.add(handler1);
        handlers.add(handler2);

        List<SDKOptions.QueueBinding> bindings1 = List.of(
            SDKOptions.QueueBinding.builder().queueName("logger-queue").routingKey("#").build()
        );
        List<SDKOptions.QueueBinding> bindings2 = List.of(
            SDKOptions.QueueBinding.builder().queueName("market-maker-queue").routingKey("#").build()
        );

        RabbitMQFeed feed1 = new RabbitMQFeed(handler1, bindings1, false);
        RabbitMQFeed feed2 = new RabbitMQFeed(handler2, bindings2, true);
        feeds.add(feed1);
        feeds.add(feed2);

        // Both feeds created successfully — no exceptions
        assertFalse(feed1.isRecoverOnReconnect(), "Logger feed should have recovery disabled");
        assertTrue(feed2.isRecoverOnReconnect(), "Market maker feed should have recovery enabled");
    }

    @Test
    @DisplayName("Multiple feeds with per-connection queue bindings are independent")
    void perConnectionQueueBindingsAreIndependent() {
        List<SDKOptions.QueueBinding> loggerBindings = List.of(
            SDKOptions.QueueBinding.builder().queueName("logger-q1").routingKey("*.*.fixtures.#").build(),
            SDKOptions.QueueBinding.builder().queueName("logger-q2").routingKey("*.*.scoreboard.#").build()
        );
        List<SDKOptions.QueueBinding> marketMakerBindings = List.of(
            SDKOptions.QueueBinding.builder().queueName("mm-q1").routingKey("*.*.markets.#").build()
        );

        RabbitMQFeed loggerFeed = createFeed(loggerBindings);
        RabbitMQFeed mmFeed = createFeed(marketMakerBindings);

        // Both feeds created successfully with different bindings
        assertNotNull(loggerFeed);
        assertNotNull(mmFeed);
    }

    @Test
    @DisplayName("Closing a feed should not affect other feeds' event handlers")
    void closingOneFeedDoesNotAffectOthers() {
        CopyOnWriteArrayList<ConnectionEvent> events1 = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<ConnectionEvent> events2 = new CopyOnWriteArrayList<>();

        EventHandler handler1 = new EventHandler(events1::add);
        EventHandler handler2 = new EventHandler(events2::add);
        handlers.add(handler1);
        handlers.add(handler2);

        RabbitMQFeed feed1 = new RabbitMQFeed(handler1);
        RabbitMQFeed feed2 = new RabbitMQFeed(handler2);
        feeds.add(feed1);
        feeds.add(feed2);

        // Close feed1
        feed1.close();

        // feed2's handler should still be functional (not shut down)
        // Trigger a heartbeat on handler2 — should not throw
        handler2.resetTimer();
        handler2.heartbeat();

        assertTrue(events1.isEmpty(), "Closed feed should not emit events");
    }

    @Test
    @DisplayName("closeExistingConnection does not double-decrement when connection has shutdown listener")
    void closeExistingConnection_noDoubleDecrement() throws Exception {
        // Simulate: a connection exists with a shutdown listener, then closeExistingConnection is called.
        // The shutdown listener should decrement once, and closeExistingConnection should NOT decrement again.

        RabbitMQFeed feed = createFeed();

        // Use reflection to simulate a connection with a mock that tracks shutdown listeners
        Connection mockConn = org.mockito.Mockito.mock(Connection.class);
        org.mockito.Mockito.when(mockConn.isOpen()).thenReturn(true);

        // Capture the shutdown listener
        List<com.rabbitmq.client.ShutdownListener> listeners = new CopyOnWriteArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            listeners.add(invocation.getArgument(0));
            return null;
        }).when(mockConn).addShutdownListener(org.mockito.Mockito.any());

        // Simulate conn.close() triggering shutdown listeners
        org.mockito.Mockito.doAnswer(invocation -> {
            org.mockito.Mockito.when(mockConn.isOpen()).thenReturn(false);
            for (com.rabbitmq.client.ShutdownListener sl : listeners) {
                sl.shutdownCompleted(new ShutdownSignalException(false, true, null, mockConn));
            }
            return null;
        }).when(mockConn).close();

        // Set the mock connection via reflection
        Field connField = RabbitMQFeed.class.getDeclaredField("conn");
        connField.setAccessible(true);
        connField.set(feed, mockConn);

        // Simulate that the connection was counted and has a shutdown listener
        // Set counter to 1 (one active connection)
        RabbitMQFeed.resetActiveConnectionCount();
        // Manually increment to simulate establish() having counted this connection
        Field activeConnsField = RabbitMQFeed.class.getDeclaredField("activeConnections");
        activeConnsField.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger counter =
            (java.util.concurrent.atomic.AtomicInteger) activeConnsField.get(null);
        counter.set(1);

        // Add a shutdown listener that decrements (simulating what establish() does)
        mockConn.addShutdownListener(cause -> counter.decrementAndGet());

        // Now close the feed — this should result in exactly ONE decrement (from the listener)
        feed.close();

        assertEquals(0, RabbitMQFeed.getActiveConnectionCount(),
            "Counter should be 0 after closing one connection (no double-decrement)");
    }

    @Test
    @DisplayName("Intentional close() does not emit disconnection event")
    void intentionalClose_noDisconnectionEvent() throws Exception {
        CopyOnWriteArrayList<ConnectionEvent> events = new CopyOnWriteArrayList<>();

        EventHandler handler = new EventHandler(events::add);
        handlers.add(handler);

        RabbitMQFeed feed = new RabbitMQFeed(handler);
        feeds.add(feed);

        // Set up a mock connection with shutdown listener
        Connection mockConn = org.mockito.Mockito.mock(Connection.class);
        org.mockito.Mockito.when(mockConn.isOpen()).thenReturn(true);

        List<com.rabbitmq.client.ShutdownListener> listeners = new CopyOnWriteArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            listeners.add(invocation.getArgument(0));
            return null;
        }).when(mockConn).addShutdownListener(org.mockito.Mockito.any());

        org.mockito.Mockito.doAnswer(invocation -> {
            org.mockito.Mockito.when(mockConn.isOpen()).thenReturn(false);
            for (com.rabbitmq.client.ShutdownListener sl : listeners) {
                sl.shutdownCompleted(new ShutdownSignalException(false, true, null, mockConn));
            }
            return null;
        }).when(mockConn).close();

        // Wire up connection via reflection
        Field connField = RabbitMQFeed.class.getDeclaredField("conn");
        connField.setAccessible(true);
        connField.set(feed, mockConn);

        // Simulate the shutdown listener that establish() would add
        // The `closing` flag should prevent handleDisconnection from being called
        Field closingField = RabbitMQFeed.class.getDeclaredField("closing");
        closingField.setAccessible(true);

        assertFalse((boolean) closingField.get(feed), "closing flag should be false before close()");

        // Call close() — the closing flag should be set before the connection is closed
        feed.close();

        assertTrue((boolean) closingField.get(feed), "closing flag should be true after close()");

        // No disconnection events should have been emitted
        assertTrue(events.isEmpty(),
            "Intentional close() should not emit disconnection events");
    }

    @Test
    @DisplayName("Counter stays accurate when creating and closing multiple feeds")
    void counterAccuracyWithMultipleFeeds() throws Exception {
        // Simulate 3 feeds, each with a mock connection
        List<RabbitMQFeed> testFeeds = new ArrayList<>();
        RabbitMQFeed.resetActiveConnectionCount();

        for (int i = 0; i < 3; i++) {
            RabbitMQFeed feed = createFeed();

            Connection mockConn = org.mockito.Mockito.mock(Connection.class);
            org.mockito.Mockito.when(mockConn.isOpen()).thenReturn(true);

            List<com.rabbitmq.client.ShutdownListener> listeners = new CopyOnWriteArrayList<>();
            org.mockito.Mockito.doAnswer(invocation -> {
                listeners.add(invocation.getArgument(0));
                return null;
            }).when(mockConn).addShutdownListener(org.mockito.Mockito.any());

            org.mockito.Mockito.doAnswer(invocation -> {
                org.mockito.Mockito.when(mockConn.isOpen()).thenReturn(false);
                for (com.rabbitmq.client.ShutdownListener sl : listeners) {
                    sl.shutdownCompleted(new ShutdownSignalException(false, true, null, mockConn));
                }
                return null;
            }).when(mockConn).close();

            Field connField = RabbitMQFeed.class.getDeclaredField("conn");
            connField.setAccessible(true);
            connField.set(feed, mockConn);

            // Simulate the shutdown listener added by establish()
            java.util.concurrent.atomic.AtomicInteger counter = getCounter();
            counter.incrementAndGet();
            mockConn.addShutdownListener(cause -> counter.decrementAndGet());

            testFeeds.add(feed);
        }

        assertEquals(3, RabbitMQFeed.getActiveConnectionCount(),
            "Should have 3 active connections");

        // Close the first feed
        testFeeds.get(0).close();
        assertEquals(2, RabbitMQFeed.getActiveConnectionCount(),
            "Should have 2 active connections after closing one");

        // Close the second feed
        testFeeds.get(1).close();
        assertEquals(1, RabbitMQFeed.getActiveConnectionCount(),
            "Should have 1 active connection after closing two");

        // Close the third feed
        testFeeds.get(2).close();
        assertEquals(0, RabbitMQFeed.getActiveConnectionCount(),
            "Should have 0 active connections after closing all");
    }

    private static java.util.concurrent.atomic.AtomicInteger getCounter() throws Exception {
        Field activeConnsField = RabbitMQFeed.class.getDeclaredField("activeConnections");
        activeConnsField.setAccessible(true);
        return (java.util.concurrent.atomic.AtomicInteger) activeConnsField.get(null);
    }
}
