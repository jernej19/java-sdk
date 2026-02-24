package com.pandascore.sdk.events;

import com.pandascore.sdk.model.feed.fixtures.FixtureMatch;
import com.pandascore.sdk.model.feed.markets.MarketsRecoveryMatch;

import java.util.Collections;
import java.util.List;

/**
 * Event emitted on disconnection or reconnection.
 * <p>
 * Code 100 = disconnection, Code 101 = reconnection.
 */
public final class ConnectionEvent {

    /** Disconnection notification code. */
    public static final int CODE_DISCONNECTION = 100;
    /** Reconnection notification code. */
    public static final int CODE_RECONNECTION = 101;

    private final int code;
    private final RecoveryData recoveryData;

    private ConnectionEvent(int code, RecoveryData recoveryData) {
        this.code = code;
        this.recoveryData = recoveryData;
    }

    /** Creates a disconnection event (code 100). */
    public static ConnectionEvent disconnection() {
        return new ConnectionEvent(CODE_DISCONNECTION, null);
    }

    /** Creates a reconnection event (code 101) with recovery data. */
    public static ConnectionEvent reconnection(RecoveryData recoveryData) {
        return new ConnectionEvent(CODE_RECONNECTION, recoveryData);
    }

    public int getCode() {
        return code;
    }

    /**
     * Recovery data from the two recovery API calls.
     * Present only on reconnection events; null for disconnection events.
     */
    public RecoveryData getRecoveryData() {
        return recoveryData;
    }

    /**
     * Holds the results of the two recovery API calls made during reconnection.
     */
    public static final class RecoveryData {
        private final List<MarketsRecoveryMatch> markets;
        private final List<FixtureMatch> matches;

        public RecoveryData(List<MarketsRecoveryMatch> markets, List<FixtureMatch> matches) {
            this.markets = markets != null ? Collections.unmodifiableList(markets) : Collections.emptyList();
            this.matches = matches != null ? Collections.unmodifiableList(matches) : Collections.emptyList();
        }

        /** Markets recovered since disconnection. */
        public List<MarketsRecoveryMatch> getMarkets() {
            return markets;
        }

        /** Matches modified during the disconnection window. */
        public List<FixtureMatch> getMatches() {
            return matches;
        }
    }

    @Override
    public String toString() {
        String type = code == CODE_DISCONNECTION ? "disconnection" : "reconnection";
        return "ConnectionEvent{code=" + code + ", type=" + type + "}";
    }
}
