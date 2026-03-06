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

    private void setReady(boolean val) throws Exception {
        Field f = EventHandler.class.getDeclaredField("ready");
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
        setReady(true);
        setLastBeat(Instant.now().minus(Duration.ofMinutes(5)));
        handler.heartbeat();
        // After heartbeat, lastBeat is reset to now; checkHeartbeat must not fire
        invokeCheckHeartbeat();
        assertTrue(events.isEmpty(), "No disconnection should fire after heartbeat");
    }

    // ============================================================
    //  2. Missed-beat counter increments and triggers at >= 3
    // ============================================================

    @Test
    @DisplayName("checkHeartbeat increments missedHeartbeats when beat is overdue (> interval + grace)")
    void checkHeartbeat_incrementsMissedCounter() throws Exception {
        setReady(true);
        // 16s > HEARTBEAT_INTERVAL(10) + HEARTBEAT_GRACE(5)
        setLastBeat(Instant.now().minus(Duration.ofSeconds(16)));
        invokeCheckHeartbeat();

        assertEquals(1, handler.getMissedHeartbeats());
        assertFalse(handler.isDisconnected(), "Should not disconnect after 1 missed beat");
        assertTrue(events.isEmpty(), "No event after 1 missed beat");
    }

    @Test
    @DisplayName("checkHeartbeat does not increment when beat is within grace period")
    void checkHeartbeat_doesNotIncrementWithinGrace() throws Exception {
        setReady(true);
        // 11s is past HEARTBEAT_INTERVAL but within the grace buffer — no miss yet
        setLastBeat(Instant.now().minus(Duration.ofSeconds(11)));
        invokeCheckHeartbeat();
        assertEquals(0, handler.getMissedHeartbeats());
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("checkHeartbeat does not increment when beat is recent")
    void checkHeartbeat_doesNotIncrementWhenRecent() throws Exception {
        setReady(true);
        // lastBeat is recent (set in constructor)
        invokeCheckHeartbeat();
        assertEquals(0, handler.getMissedHeartbeats());
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("Disconnection triggers only after 3 consecutive missed beats")
    void disconnection_triggersAfterThreeMissedBeats() throws Exception {
        setReady(true);
        setLastBeat(Instant.now().minus(Duration.ofSeconds(16)));

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
        setReady(true);
        setLastBeat(Instant.now().minus(Duration.ofSeconds(16)));

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
        setReady(true);
        setLastBeat(Instant.now().minus(Duration.ofSeconds(16)));

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
        setReady(true);
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
        setReady(true);
        setLastBeat(Instant.now().minus(Duration.ofSeconds(16)));
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
    //  6. resetTimer does NOT trigger recovery — and sets ready
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
    @DisplayName("resetTimer() marks the handler as ready for heartbeat counting")
    void resetTimer_setsReady() {
        assertFalse(handler.isReady(), "Should not be ready before resetTimer()");
        handler.resetTimer();
        assertTrue(handler.isReady(), "Should be ready after resetTimer()");
    }

    @Test
    @DisplayName("resetTimer() prevents stale missed-beat accumulation after AMQP reconnect")
    void resetTimer_preventsStaleMisses() throws Exception {
        // Simulate: lastBeat is old, 2 beats already missed
        setLastBeat(Instant.now().minus(Duration.ofSeconds(25)));
        setMissedHeartbeats(2);

        // AMQP reconnects — resetTimer called (also sets ready=true)
        handler.resetTimer();

        // Now checkHeartbeat should see fresh lastBeat, no increment
        invokeCheckHeartbeat();
        assertEquals(0, handler.getMissedHeartbeats());
        assertFalse(handler.isDisconnected());
        assertTrue(events.isEmpty());
    }

    // ============================================================
    //  7. ready flag — checkHeartbeat is a no-op before connection
    // ============================================================

    @Test
    @DisplayName("checkHeartbeat does nothing before connection is established (ready=false)")
    void checkHeartbeat_doesNothing_beforeReady() throws Exception {
        // ready starts as false; set an old lastBeat to prove the check is skipped
        setLastBeat(Instant.now().minus(Duration.ofSeconds(60)));
        setMissedHeartbeats(0);

        invokeCheckHeartbeat();
        invokeCheckHeartbeat();
        invokeCheckHeartbeat();

        assertEquals(0, handler.getMissedHeartbeats(), "No misses counted before ready");
        assertFalse(handler.isDisconnected());
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("checkHeartbeat starts counting only after resetTimer() is called")
    void checkHeartbeat_startsAfterResetTimer() throws Exception {
        // Before resetTimer — checkHeartbeat is a no-op even with a very old lastBeat
        setLastBeat(Instant.now().minus(Duration.ofSeconds(60)));
        invokeCheckHeartbeat();
        assertEquals(0, handler.getMissedHeartbeats());

        // resetTimer() marks ready and refreshes lastBeat to now
        handler.resetTimer();
        assertTrue(handler.isReady());

        // Subsequent check sees recent lastBeat — no miss
        invokeCheckHeartbeat();
        assertEquals(0, handler.getMissedHeartbeats());
        assertTrue(events.isEmpty());
    }

    // ============================================================
    //  8. Grace period (HEARTBEAT_GRACE = 5s)
    // ============================================================

    @Test
    @DisplayName("HEARTBEAT_GRACE constant is 5 seconds")
    void heartbeatGrace_is5seconds() {
        assertEquals(Duration.ofSeconds(5), EventHandler.HEARTBEAT_GRACE);
    }

    @Test
    @DisplayName("Beat arriving just after interval but within grace is NOT counted as missed")
    void grace_withinGrace_notMissed() throws Exception {
        setReady(true);
        // 14s = interval(10) + 4s — inside grace window (grace = 5s, threshold = 15s)
        setLastBeat(Instant.now().minus(Duration.ofSeconds(14)));
        invokeCheckHeartbeat();
        assertEquals(0, handler.getMissedHeartbeats());
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("Beat past interval + grace IS counted as missed")
    void grace_pastGrace_counted() throws Exception {
        setReady(true);
        // 16s > interval(10) + grace(5) = 15s
        setLastBeat(Instant.now().minus(Duration.ofSeconds(16)));
        invokeCheckHeartbeat();
        assertEquals(1, handler.getMissedHeartbeats());
    }

    // ============================================================
    //  9. recoverOnReconnect flag is respected
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
    // 10. Timer interval and miss-count constants
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
    // 11. Recovery failure does not prevent reconnection event
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
    // 12. Full lifecycle: connect → disconnect → reconnect
    // ============================================================

    @Test
    @DisplayName("Full lifecycle: normal → missed beats → disconnection → heartbeat → reconnection")
    void fullLifecycle() throws Exception {
        // Phase 0: connection established — resetTimer marks ready
        handler.resetTimer();

        // Phase 1: Normal operation — no events
        invokeCheckHeartbeat();
        assertTrue(events.isEmpty());

        // Phase 2: Heartbeats stop arriving
        setLastBeat(Instant.now().minus(Duration.ofSeconds(16)));
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
