package com.pandascore.sdk.events;

import com.pandascore.sdk.model.feed.fixtures.FixtureMatch;
import com.pandascore.sdk.model.feed.markets.MarketsRecoveryMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConnectionEvent — factory methods, codes, and RecoveryData.
 */
class ConnectionEventTest {

    // ============================================================
    //  Constants
    // ============================================================

    @Test
    @DisplayName("CODE_DISCONNECTION is 100")
    void codeDisconnection_is100() {
        assertEquals(100, ConnectionEvent.CODE_DISCONNECTION);
    }

    @Test
    @DisplayName("CODE_RECONNECTION is 101")
    void codeReconnection_is101() {
        assertEquals(101, ConnectionEvent.CODE_RECONNECTION);
    }

    // ============================================================
    //  Disconnection factory
    // ============================================================

    @Test
    @DisplayName("disconnection() creates event with code 100 and null recovery data")
    void disconnection_createsCorrectEvent() {
        ConnectionEvent evt = ConnectionEvent.disconnection();
        assertEquals(100, evt.getCode());
        assertNull(evt.getRecoveryData());
    }

    // ============================================================
    //  Reconnection factory
    // ============================================================

    @Test
    @DisplayName("reconnection() creates event with code 101 and recovery data")
    void reconnection_createsCorrectEvent() {
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(
            Collections.emptyList(), Collections.emptyList());
        ConnectionEvent evt = ConnectionEvent.reconnection(data);
        assertEquals(101, evt.getCode());
        assertNotNull(evt.getRecoveryData());
    }

    @Test
    @DisplayName("reconnection with populated recovery data preserves markets and matches")
    void reconnection_preservesRecoveryData() {
        List<MarketsRecoveryMatch> markets = List.of(new MarketsRecoveryMatch(), new MarketsRecoveryMatch());
        List<FixtureMatch> matches = List.of(new FixtureMatch());

        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(markets, matches);
        ConnectionEvent evt = ConnectionEvent.reconnection(data);

        assertEquals(2, evt.getRecoveryData().getMarkets().size());
        assertEquals(1, evt.getRecoveryData().getMatches().size());
    }

    // ============================================================
    //  RecoveryData
    // ============================================================

    @Test
    @DisplayName("RecoveryData with null lists returns empty lists")
    void recoveryData_nullLists_returnsEmpty() {
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(null, null);
        assertNotNull(data.getMarkets());
        assertNotNull(data.getMatches());
        assertTrue(data.getMarkets().isEmpty());
        assertTrue(data.getMatches().isEmpty());
    }

    @Test
    @DisplayName("RecoveryData lists are unmodifiable")
    void recoveryData_listsAreUnmodifiable() {
        List<MarketsRecoveryMatch> markets = List.of(new MarketsRecoveryMatch());
        List<FixtureMatch> matches = List.of(new FixtureMatch());
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(markets, matches);

        assertThrows(UnsupportedOperationException.class,
            () -> data.getMarkets().add(new MarketsRecoveryMatch()));
        assertThrows(UnsupportedOperationException.class,
            () -> data.getMatches().add(new FixtureMatch()));
    }

    // ============================================================
    //  RecoveryData.isComplete
    // ============================================================

    @Test
    @DisplayName("RecoveryData 2-arg constructor defaults isComplete to true")
    void recoveryData_twoArgConstructor_defaultsComplete() {
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(
            Collections.emptyList(), Collections.emptyList());
        assertTrue(data.isComplete());
    }

    @Test
    @DisplayName("RecoveryData with complete=true reports isComplete true")
    void recoveryData_completeTrue() {
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(
            Collections.emptyList(), Collections.emptyList(), true);
        assertTrue(data.isComplete());
    }

    @Test
    @DisplayName("RecoveryData with complete=false reports isComplete false")
    void recoveryData_completeFalse() {
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(
            Collections.emptyList(), Collections.emptyList(), false);
        assertFalse(data.isComplete());
    }

    // ============================================================
    //  toString
    // ============================================================

    @Test
    @DisplayName("toString for disconnection includes type=disconnection")
    void toString_disconnection() {
        ConnectionEvent evt = ConnectionEvent.disconnection();
        String str = evt.toString();
        assertTrue(str.contains("100"));
        assertTrue(str.contains("disconnection"));
    }

    @Test
    @DisplayName("toString for reconnection includes type=reconnection")
    void toString_reconnection() {
        ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(null, null);
        ConnectionEvent evt = ConnectionEvent.reconnection(data);
        String str = evt.toString();
        assertTrue(str.contains("101"));
        assertTrue(str.contains("reconnection"));
    }
}
