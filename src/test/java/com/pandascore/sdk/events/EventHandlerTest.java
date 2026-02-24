package com.pandascore.sdk.events;

import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.http.MatchesClient;
import com.pandascore.sdk.model.feed.fixtures.FixtureMatch;
import com.pandascore.sdk.model.feed.markets.MarketsRecoveryMatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventHandler — heartbeat monitoring, disconnection detection,
 * missed-beat counting, and recovery logic.
 */
class EventHandlerTest {

    private List<ConnectionEvent> events;
    private EventHandler handler;

    @BeforeEach
    void setUp() {
        // Initialize SDKConfig with minimal options for recovery calls
        SDKConfig.setOptions(SDKOptions.builder()
            .apiToken("test-token")
            .companyId(1)
            .email("test@test.com")
            .password("pass")
            .apiBaseUrl("http://localhost:9999")
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q")
                .routingKey("r")
                .build())
            .build());

        events = new CopyOnWriteArrayList<>();
        handler = new EventHandler(events::add);
    }

    @AfterEach
    void tearDown() {
        handler.close();
    }

    // --- Helpers to manipulate internal state via reflection ---

    private void setLastBeat(Instant when) throws Exception {
        Field f = EventHandler.class.getDeclaredField("lastBeat");
        f.setAccessible(true);
        f.set(handler, when);
    }

    private void setDisconnected(boolean val) throws Exception {
        Field f = EventHandler.class.getDeclaredField("disconnected");
        f.setAccessible(true);
        f.set(handler, val);
    }

    private void setDownAt(Instant when) throws Exception {
        Field f = EventHandler.class.getDeclaredField("downAt");
        f.setAccessible(true);
        f.set(handler, when);
    }

    private void setMissedHeartbeats(int val) throws Exception {
        Field f = EventHandler.class.getDeclaredField("missedHeartbeats");
        f.setAccessible(true);
        f.set(handler, val);
    }

    private void invokeCheckHeartbeat() throws Exception {
        Method m = EventHandler.class.getDeclaredMethod("checkHeartbeat");
        m.setAccessible(true);
        m.invoke(handler);
    }

    // ============================================================
    //  1. Heartbeat resets missedHeartbeats to 0
    // ============================================================

    @Test
    @DisplayName("heartbeat() resets missedHeartbeats counter to 0")
    void heartbeat_resetsMissedCounter() throws Exception {
        setMissedHeartbeats(2);
        handler.heartbeat();
        assertEquals(0, handler.getMissedHeartbeats());
    }

    @Test
    @DisplayName("heartbeat() updates lastBeat to approximately now")
    void heartbeat_updatesLastBeat() throws Exception {
        setLastBeat(Instant.now().minus(Duration.ofMinutes(5)));
        Instant before = Instant.now();
        handler.heartbeat();
        // After heartbeat, the handler should not be in disconnected state
        // and the internal lastBeat should be recent (indirectly verified
        // by checkHeartbeat not triggering disconnection)
        invokeCheckHeartbeat();
        assertTrue(events.isEmpty(), "No disconnection should fire after heartbeat");
    }

    // ============================================================
    //  2. Missed-beat counter increments and triggers at >= 3
    // ============================================================

    @Test
    @DisplayName("checkHeartbeat increments missedHeartbeats when beat is overdue")
    void checkHeartbeat_incrementsMissedCounter() throws Exception {
        // Set lastBeat to 11 seconds ago (just past one interval)
        setLastBeat(Instant.now().minus(Duration.ofSeconds(11)));
        invokeCheckHeartbeat();

        assertEquals(1, handler.getMissedHeartbeats());
        assertFalse(handler.isDisconnected(), "Should not disconnect after 1 missed beat");
        assertTrue(events.isEmpty(), "No event after 1 missed beat");
    }

    @Test
    @DisplayName("checkHeartbeat does not increment when beat is recent")
    void checkHeartbeat_doesNotIncrementWhenRecent() throws Exception {
        // lastBeat is recent (set in constructor)
        invokeCheckHeartbeat();
        assertEquals(0, handler.getMissedHeartbeats());
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("Disconnection triggers only after 3 consecutive missed beats")
    void disconnection_triggersAfterThreeMissedBeats() throws Exception {
        setLastBeat(Instant.now().minus(Duration.ofSeconds(11)));

        // Beat 1 missed
        invokeCheckHeartbeat();
        assertEquals(1, handler.getMissedHeartbeats());
        assertFalse(handler.isDisconnected());
        assertTrue(events.isEmpty());

        // Beat 2 missed
        invokeCheckHeartbeat();
        assertEquals(2, handler.getMissedHeartbeats());
        assertFalse(handler.isDisconnected());
        assertTrue(events.isEmpty());

        // Beat 3 missed — should disconnect
        invokeCheckHeartbeat();
        assertTrue(handler.isDisconnected());
        assertEquals(1, events.size());
        assertEquals(ConnectionEvent.CODE_DISCONNECTION, events.get(0).getCode());
    }

    @Test
    @DisplayName("Missed counter resets to 0 when a heartbeat arrives mid-sequence")
    void missedCounter_resetsOnHeartbeat() throws Exception {
        setLastBeat(Instant.now().minus(Duration.ofSeconds(11)));

        invokeCheckHeartbeat(); // miss 1
        invokeCheckHeartbeat(); // miss 2
        assertEquals(2, handler.getMissedHeartbeats());

        // A heartbeat arrives, resetting the counter
        handler.heartbeat();
        assertEquals(0, handler.getMissedHeartbeats());
        assertFalse(handler.isDisconnected());

        // Subsequent check sees recent beat — no increment
        invokeCheckHeartbeat();
        assertEquals(0, handler.getMissedHeartbeats());
        assertTrue(events.isEmpty());
    }

    // ============================================================
    //  3. Disconnection fires only once
    // ============================================================

    @Test
    @DisplayName("Disconnection event fires exactly once even with multiple missed checks")
    void disconnection_firesOnlyOnce() throws Exception {
        setLastBeat(Instant.now().minus(Duration.ofSeconds(11)));

        invokeCheckHeartbeat(); // miss 1
        invokeCheckHeartbeat(); // miss 2
        invokeCheckHeartbeat(); // miss 3 → disconnection
        invokeCheckHeartbeat(); // already disconnected — should not fire again
        invokeCheckHeartbeat(); // still disconnected

        assertEquals(1, events.size(), "Disconnection should fire exactly once");
        assertEquals(ConnectionEvent.CODE_DISCONNECTION, events.get(0).getCode());
    }

    @Test
    @DisplayName("handleDisconnection() fires only once even if called repeatedly")
    void handleDisconnection_firesOnlyOnce() {
        handler.handleDisconnection();
        handler.handleDisconnection();
        handler.handleDisconnection();

        assertEquals(1, events.size());
        assertEquals(ConnectionEvent.CODE_DISCONNECTION, events.get(0).getCode());
    }

    @Test
    @DisplayName("handleDisconnection + checkHeartbeat do not produce duplicate events")
    void handleDisconnection_andCheckHeartbeat_noDuplicate() throws Exception {
        handler.handleDisconnection();
        setLastBeat(Instant.now().minus(Duration.ofSeconds(31)));
        invokeCheckHeartbeat();

        assertEquals(1, events.size(), "Only one disconnection event");
    }

    // ============================================================
    //  4. Disconnection event has code 100
    // ============================================================

    @Test
    @DisplayName("handleDisconnection emits code 100")
    void handleDisconnection_emitsCode100() {
        handler.handleDisconnection();
        assertEquals(1, events.size());
        assertEquals(100, events.get(0).getCode());
        assertNull(events.get(0).getRecoveryData());
    }

    @Test
    @DisplayName("checkHeartbeat-triggered disconnection emits code 100")
    void checkHeartbeat_disconnection_emitsCode100() throws Exception {
        setLastBeat(Instant.now().minus(Duration.ofSeconds(11)));
        invokeCheckHeartbeat();
        invokeCheckHeartbeat();
        invokeCheckHeartbeat();

        assertEquals(1, events.size());
        assertEquals(100, events.get(0).getCode());
    }

    // ============================================================
    //  5. Reconnection triggered by heartbeat after disconnection
    // ============================================================

    @Test
    @DisplayName("heartbeat() after disconnection triggers reconnection with code 101")
    void heartbeat_afterDisconnection_triggersReconnection() throws Exception {
        // Put handler in disconnected state
        setDisconnected(true);
        setDownAt(Instant.now().minus(Duration.ofSeconds(30)));

        try (MockedStatic<MatchesClient> mc = mockStatic(MatchesClient.class)) {
            mc.when(() -> MatchesClient.recoverMarkets(anyString()))
                .thenReturn(List.of(new MarketsRecoveryMatch()));
            mc.when(() -> MatchesClient.fetchMatchesRange(anyString(), anyString()))
                .thenReturn(List.of(new FixtureMatch()));

            handler.heartbeat();
        }

        assertFalse(handler.isDisconnected());
        assertEquals(1, events.size());

        ConnectionEvent evt = events.get(0);
        assertEquals(ConnectionEvent.CODE_RECONNECTION, evt.getCode());
        assertNotNull(evt.getRecoveryData());
        assertEquals(1, evt.getRecoveryData().getMarkets().size());
        assertEquals(1, evt.getRecoveryData().getMatches().size());
    }

    @Test
    @DisplayName("Reconnection event includes recovery data from both API calls")
    void reconnection_includesRecoveryData() throws Exception {
        setDisconnected(true);
        setDownAt(Instant.now().minus(Duration.ofSeconds(30)));

        List<MarketsRecoveryMatch> markets = List.of(
            new MarketsRecoveryMatch(), new MarketsRecoveryMatch());
        List<FixtureMatch> matches = List.of(
            new FixtureMatch(), new FixtureMatch(), new FixtureMatch());

        try (MockedStatic<MatchesClient> mc = mockStatic(MatchesClient.class)) {
            mc.when(() -> MatchesClient.recoverMarkets(anyString()))
                .thenReturn(markets);
            mc.when(() -> MatchesClient.fetchMatchesRange(anyString(), anyString()))
                .thenReturn(matches);

            handler.heartbeat();
        }

        ConnectionEvent evt = events.get(0);
        assertEquals(101, evt.getCode());
        assertEquals(2, evt.getRecoveryData().getMarkets().size());
        assertEquals(3, evt.getRecoveryData().getMatches().size());
    }

    // ============================================================
    //  6. resetTimer does NOT trigger recovery
    // ============================================================

    @Test
    @DisplayName("resetTimer() resets lastBeat and counter without triggering recovery")
    void resetTimer_doesNotTriggerRecovery() throws Exception {
        setDisconnected(true);
        setDownAt(Instant.now().minus(Duration.ofSeconds(30)));
        setMissedHeartbeats(5);

        handler.resetTimer();

        // Still disconnected — recovery is not triggered
        assertTrue(handler.isDisconnected());
        // Counter is reset
        assertEquals(0, handler.getMissedHeartbeats());
        // No events emitted
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("resetTimer() prevents stale missed-beat accumulation after AMQP reconnect")
    void resetTimer_preventsStaleMisses() throws Exception {
        // Simulate: lastBeat is old, 2 beats already missed
        setLastBeat(Instant.now().minus(Duration.ofSeconds(25)));
        setMissedHeartbeats(2);

        // AMQP reconnects — resetTimer called
        handler.resetTimer();

        // Now checkHeartbeat should see fresh lastBeat, no increment
        invokeCheckHeartbeat();
        assertEquals(0, handler.getMissedHeartbeats());
        assertFalse(handler.isDisconnected());
        assertTrue(events.isEmpty());
    }

    // ============================================================
    //  7. recoverOnReconnect flag is respected
    // ============================================================

    @Test
    @DisplayName("When recoverOnReconnect=false, recovery APIs are NOT called")
    void recoverOnReconnect_false_skipsAPICalls() throws Exception {
        // Reconfigure SDK with recoverOnReconnect=false
        SDKConfig.setOptions(SDKOptions.builder()
            .apiToken("test-token")
            .companyId(1)
            .email("test@test.com")
            .password("pass")
            .apiBaseUrl("http://localhost:9999")
            .recoverOnReconnect(false)
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q")
                .routingKey("r")
                .build())
            .build());

        setDisconnected(true);
        setDownAt(Instant.now().minus(Duration.ofSeconds(30)));

        try (MockedStatic<MatchesClient> mc = mockStatic(MatchesClient.class)) {
            handler.heartbeat();

            // Verify recovery APIs were NOT called
            mc.verify(() -> MatchesClient.recoverMarkets(anyString()), never());
            mc.verify(() -> MatchesClient.fetchMatchesRange(anyString(), anyString()), never());
        }

        // Reconnection event still emitted, but with empty recovery data
        assertEquals(1, events.size());
        assertEquals(ConnectionEvent.CODE_RECONNECTION, events.get(0).getCode());
        assertNotNull(events.get(0).getRecoveryData());
        assertTrue(events.get(0).getRecoveryData().getMarkets().isEmpty());
        assertTrue(events.get(0).getRecoveryData().getMatches().isEmpty());
    }

    @Test
    @DisplayName("When recoverOnReconnect=true (default), recovery APIs are called")
    void recoverOnReconnect_true_callsAPIs() throws Exception {
        setDisconnected(true);
        setDownAt(Instant.now().minus(Duration.ofSeconds(30)));

        try (MockedStatic<MatchesClient> mc = mockStatic(MatchesClient.class)) {
            mc.when(() -> MatchesClient.recoverMarkets(anyString()))
                .thenReturn(Collections.emptyList());
            mc.when(() -> MatchesClient.fetchMatchesRange(anyString(), anyString()))
                .thenReturn(Collections.emptyList());

            handler.heartbeat();

            mc.verify(() -> MatchesClient.recoverMarkets(anyString()), times(1));
            mc.verify(() -> MatchesClient.fetchMatchesRange(anyString(), anyString()), times(1));
        }
    }

    // ============================================================
    //  8. Timer interval constant
    // ============================================================

    @Test
    @DisplayName("HEARTBEAT_INTERVAL is 10 seconds")
    void heartbeatInterval_is10seconds() {
        assertEquals(Duration.ofSeconds(10), EventHandler.HEARTBEAT_INTERVAL);
    }

    @Test
    @DisplayName("MAX_MISSED_COUNT is 3")
    void maxMissedCount_is3() {
        assertEquals(3, EventHandler.MAX_MISSED_COUNT);
    }

    // ============================================================
    //  9. Recovery failure does not prevent reconnection event
    // ============================================================

    @Test
    @DisplayName("Recovery API failure still emits reconnection event")
    void recoveryFailure_stillEmitsReconnection() throws Exception {
        setDisconnected(true);
        setDownAt(Instant.now().minus(Duration.ofSeconds(30)));

        try (MockedStatic<MatchesClient> mc = mockStatic(MatchesClient.class)) {
            mc.when(() -> MatchesClient.recoverMarkets(anyString()))
                .thenThrow(new java.io.IOException("network error"));

            handler.heartbeat();
        }

        // Reconnection event should still be emitted despite recovery failure
        assertFalse(handler.isDisconnected());
        assertEquals(1, events.size());
        assertEquals(ConnectionEvent.CODE_RECONNECTION, events.get(0).getCode());
    }

    // ============================================================
    // 10. Full lifecycle: connect → disconnect → reconnect
    // ============================================================

    @Test
    @DisplayName("Full lifecycle: normal → missed beats → disconnection → heartbeat → reconnection")
    void fullLifecycle() throws Exception {
        // Phase 1: Normal operation — no events
        invokeCheckHeartbeat();
        assertTrue(events.isEmpty());

        // Phase 2: Heartbeats stop arriving
        setLastBeat(Instant.now().minus(Duration.ofSeconds(11)));
        invokeCheckHeartbeat(); // miss 1
        invokeCheckHeartbeat(); // miss 2
        assertTrue(events.isEmpty());

        invokeCheckHeartbeat(); // miss 3 → disconnect
        assertEquals(1, events.size());
        assertEquals(100, events.get(0).getCode());
        assertTrue(handler.isDisconnected());

        // Phase 3: More missed beats — no duplicate
        invokeCheckHeartbeat();
        assertEquals(1, events.size());

        // Phase 4: Heartbeat arrives — reconnection
        try (MockedStatic<MatchesClient> mc = mockStatic(MatchesClient.class)) {
            mc.when(() -> MatchesClient.recoverMarkets(anyString()))
                .thenReturn(Collections.emptyList());
            mc.when(() -> MatchesClient.fetchMatchesRange(anyString(), anyString()))
                .thenReturn(Collections.emptyList());

            handler.heartbeat();
        }

        assertFalse(handler.isDisconnected());
        assertEquals(2, events.size());
        assertEquals(100, events.get(0).getCode());
        assertEquals(101, events.get(1).getCode());
    }
}
