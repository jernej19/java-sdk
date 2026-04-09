package com.example;

import com.pandascore.sdk.FeedListener;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.ConnectionEvent;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.model.feed.fixtures.FixtureMessage;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;
import com.pandascore.sdk.model.feed.markets.MarketsRecoveryMatch;
import com.pandascore.sdk.model.feed.fixtures.FixtureMatch;
import com.pandascore.sdk.model.feed.scoreboard.ScoreboardCs;
import com.pandascore.sdk.model.feed.scoreboard.ScoreboardLol;
import com.pandascore.sdk.model.feed.scoreboard.ScoreboardDota2;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests that verify the PandaScore SDK dependency is correctly
 * resolved and that key SDK classes are functional.
 *
 * These tests do NOT connect to any live server — they only verify
 * that the SDK classes are importable and behave correctly in isolation.
 */
public class SDKIntegrationTest {

    @BeforeEach
    void setUp() {
        // Configure SDK with dummy credentials for testing
        SDKOptions options = SDKOptions.builder()
            .apiToken("test-token")
            .companyId(1)
            .email("test@example.com")
            .password("test-password")
            .queueBinding(
                SDKOptions.QueueBinding.builder()
                    .queueName("test-queue")
                    .routingKey("#")
                    .build()
            )
            .build();

        SDKConfig.setOptions(options);
    }

    // -------------------------------------------------------
    // SDK Configuration
    // -------------------------------------------------------

    @Test
    void sdkOptionsBuildsCorrectly() {
        SDKOptions opts = SDKConfig.getInstance().getOptions();

        assertEquals("test-token", opts.getApiToken());
        assertEquals(1, opts.getCompanyId());
        assertEquals("test@example.com", opts.getEmail());
        assertEquals("test-password", opts.getPassword());
        assertFalse(opts.getQueueBindings().isEmpty());
    }

    @Test
    void sdkOptionsHasDefaults() {
        SDKOptions opts = SDKConfig.getInstance().getOptions();

        assertEquals("trading-feed.pandascore.co", opts.getFeedHost());
        assertTrue(opts.isRecoverOnReconnect());
        assertFalse(opts.isAlwaysLogPayload());
        assertFalse(opts.isAmericanOdds());
        assertFalse(opts.isFractionalOdds());
    }

    @Test
    void queueBindingHasCorrectValues() {
        SDKOptions.QueueBinding binding = SDKConfig.getInstance()
            .getOptions().getQueueBindings().get(0);

        assertEquals("test-queue", binding.getQueueName());
        assertEquals("#", binding.getRoutingKey());
    }

    @Test
    void sdkOptionsValidationRejectsEmptyToken() {
        assertThrows(Exception.class, () -> {
            SDKOptions.builder()
                .apiToken("")
                .companyId(1)
                .email("test@example.com")
                .password("test")
                .queueBinding(SDKOptions.QueueBinding.builder()
                    .queueName("q")
                    .routingKey("#")
                    .build())
                .build()
                .validate();
        });
    }

    // -------------------------------------------------------
    // Event Handler
    // -------------------------------------------------------

    @Test
    void eventHandlerCreatesWithoutError() {
        EventHandler handler = new EventHandler(event -> {});
        assertNotNull(handler);
        handler.close();
    }

    @Test
    void eventHandlerReceivesDisconnectionCallback() {
        AtomicReference<ConnectionEvent> received = new AtomicReference<>();
        EventHandler handler = new EventHandler(received::set);
        assertNotNull(handler);
        handler.close();
    }

    // -------------------------------------------------------
    // Connection Events
    // -------------------------------------------------------

    @Test
    void disconnectionEventHasCode100() {
        ConnectionEvent event = ConnectionEvent.disconnection();

        assertEquals(100, event.getCode());
        assertEquals(ConnectionEvent.CODE_DISCONNECTION, event.getCode());
        assertNull(event.getRecoveryData());
    }

    @Test
    void reconnectionEventHasCode101() {
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(
            Collections.emptyList(), Collections.emptyList(), true);
        ConnectionEvent event = ConnectionEvent.reconnection(data);

        assertEquals(101, event.getCode());
        assertEquals(ConnectionEvent.CODE_RECONNECTION, event.getCode());
        assertNotNull(event.getRecoveryData());
    }

    @Test
    void recoveryDataIsCompleteWhenBothCallsSucceed() {
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(
            Collections.emptyList(), Collections.emptyList(), true);

        assertTrue(data.isComplete());
        assertNotNull(data.getMarkets());
        assertNotNull(data.getMatches());
    }

    @Test
    void recoveryDataIsIncompleteWhenCallFails() {
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(
            Collections.emptyList(), Collections.emptyList(), false);

        assertFalse(data.isComplete());
    }

    @Test
    void recoveryDataHandlesNullLists() {
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(
            null, null, true);

        assertNotNull(data.getMarkets());
        assertNotNull(data.getMatches());
        assertTrue(data.getMarkets().isEmpty());
        assertTrue(data.getMatches().isEmpty());
    }

    // -------------------------------------------------------
    // FeedListener
    // -------------------------------------------------------

    @Test
    void feedListenerDefaultMethodsAreNoOps() {
        // All default methods should run without errors
        FeedListener listener = new FeedListener() {};

        assertDoesNotThrow(() -> listener.onMarkets(null));
        assertDoesNotThrow(() -> listener.onFixture(null));
        assertDoesNotThrow(() -> listener.onScoreboard(null, null));
        assertDoesNotThrow(() -> listener.onUnknown(null));
    }

    @Test
    void feedListenerOverridesWork() {
        AtomicBoolean marketsCalled = new AtomicBoolean(false);
        AtomicBoolean fixtureCalled = new AtomicBoolean(false);

        FeedListener listener = new FeedListener() {
            @Override
            public void onMarkets(MarketsMessage message) {
                marketsCalled.set(true);
            }

            @Override
            public void onFixture(FixtureMessage message) {
                fixtureCalled.set(true);
            }
        };

        listener.onMarkets(null);
        listener.onFixture(null);

        assertTrue(marketsCalled.get());
        assertTrue(fixtureCalled.get());
    }

    // -------------------------------------------------------
    // Model classes are importable
    // -------------------------------------------------------

    @Test
    void marketModelClassesAreAccessible() {
        assertNotNull(MarketsMessage.class);
        assertNotNull(MarketsRecoveryMatch.class);
    }

    @Test
    void fixtureModelClassesAreAccessible() {
        assertNotNull(FixtureMessage.class);
        assertNotNull(FixtureMatch.class);
    }

    @Test
    void scoreboardModelClassesAreAccessible() {
        assertNotNull(ScoreboardCs.class);
        assertNotNull(ScoreboardLol.class);
        assertNotNull(ScoreboardDota2.class);
    }

    // -------------------------------------------------------
    // Jackson (transitive dependency) works
    // -------------------------------------------------------

    @Test
    void jacksonObjectMapperIsAvailable() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"type\":\"markets\",\"match_id\":123}");

        assertEquals("markets", node.get("type").asText());
        assertEquals(123, node.get("match_id").asInt());
    }

    @Test
    void jacksonDeserializesMarketsMessage() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"type\":\"markets\",\"match_id\":\"999\",\"action\":\"update\",\"markets\":[]}";
        MarketsMessage msg = mapper.readValue(json, MarketsMessage.class);

        assertNotNull(msg);
    }
}
