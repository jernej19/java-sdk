package com.pandascore.sdk.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.http.MatchesClient;
import com.pandascore.sdk.model.feed.fixtures.FixtureMatch;
import com.pandascore.sdk.model.feed.markets.MarketsRecoveryMatch;
import com.pandascore.sdk.rmq.RabbitMQFeed;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * EventHandler tracks heartbeat events and connection status.
 * It schedules periodic checks to detect missed heartbeats,
 * issues disconnection notifications, and allows graceful shutdown.
 */
public class EventHandler implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(EventHandler.class);

    // Interval between expected heartbeats
    static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(10);
    // Grace period added to HEARTBEAT_INTERVAL before counting a beat as missed.
    // Absorbs network jitter: a beat is only missed if it is more than
    // HEARTBEAT_INTERVAL + HEARTBEAT_GRACE seconds late.
    static final Duration HEARTBEAT_GRACE = Duration.ofSeconds(5);
    // Number of consecutive missed heartbeats before triggering disconnection
    static final int MAX_MISSED_COUNT = 3;

    private final ScheduledExecutorService scheduler;
    private final ScheduledFuture<?> checkTask;
    private final Consumer<ConnectionEvent> sink;
    // Lock guarding disconnected/recovery state transitions to prevent races
    // between heartbeat(), handleDisconnection(), and checkHeartbeat().
    private final Object stateLock = new Object();

    // Last time a heartbeat was received
    private volatile Instant lastBeat;
    // Number of consecutive missed heartbeats
    private volatile int missedHeartbeats;
    // Whether currently marked as disconnected
    private volatile boolean disconnected;
    // Whether the connection has been fully established at least once.
    // checkHeartbeat() is a no-op until resetTimer() marks us ready,
    // preventing false disconnection events during the initial dial-up.
    private volatile boolean ready;
    // Timestamp when disconnection was detected
    private volatile Instant downAt;
    // RabbitMQ feed reference (for controlling message buffering during recovery)
    private volatile RabbitMQFeed feed;

    /**
     * @param sink Consumer notified with ConnectionEvent on disconnection (code 100) or reconnection (code 101)
     */
    public EventHandler(Consumer<ConnectionEvent> sink) {
        this.sink = sink;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "beat-watch");
            t.setDaemon(true);
            return t;
        });

        this.lastBeat = Instant.now();
        this.missedHeartbeats = 0;
        this.disconnected = false;
        this.ready = false;
        this.downAt = null;

        // Schedule periodic check for missed heartbeats
        Map<String, String> savedMap = MDC.getCopyOfContextMap();
        this.checkTask = scheduler.scheduleAtFixedRate(
            () -> {
                if (savedMap != null) MDC.setContextMap(savedMap);
                this.checkHeartbeat();
            },
            HEARTBEAT_INTERVAL.toMillis(),
            HEARTBEAT_INTERVAL.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Sets the RabbitMQ feed reference for controlling message buffering during recovery.
     * Must be called after construction to enable recovery mode.
     *
     * @param feed the RabbitMQFeed instance
     */
    public void setFeed(RabbitMQFeed feed) {
        this.feed = feed;
    }

    /**
     * Returns the connection label from the associated feed, or an empty string if no feed is set.
     */
    private String label() {
        RabbitMQFeed f = feed;
        return f != null ? f.getConnectionLabel() + " " : "";
    }

    /**
     * Call when a heartbeat message is received to reset the timer and missed-beat counter.
     * If previously disconnected, initiates recovery process before notifying application.
     */
    public void heartbeat() {
        lastBeat = Instant.now();
        missedHeartbeats = 0;
        synchronized (stateLock) {
            if (!disconnected) return;
            disconnected = false;
            Instant up = lastBeat;
            MDC.put("operation", "reconnect");
            logger.info("{}Heartbeat restored - starting recovery", label());

            // Start buffering messages during recovery
            if (feed != null) {
                feed.startRecovery();
            }

            List<MarketsRecoveryMatch> recoveredMarkets = Collections.emptyList();
            List<FixtureMatch> recoveredMatches = Collections.emptyList();
            boolean recoveryComplete = false;

            try {
                boolean recover = feed != null ? feed.isRecoverOnReconnect()
                    : SDKConfig.getInstance().getOptions().isRecoverOnReconnect();
                if (recover && downAt != null) {
                    recoveredMarkets = MatchesClient.recoverMarkets(downAt.toString());
                    recoveredMatches = MatchesClient.fetchMatchesRange(downAt.toString(), up.toString());
                }
                recoveryComplete = true;
                logger.info("{}Recovery complete - reconnection successful", label());
            } catch (Exception e) {
                logger.error("{}Automatic recovery failed - data may be incomplete", label(), e);
            } finally {
                downAt = null;
            }

            // End recovery: process buffered messages then resume normal operation
            if (feed != null) {
                feed.endRecovery();
            }

            // Notify application AFTER recovery is complete AND buffered messages are processed
            ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(
                recoveredMarkets, recoveredMatches, recoveryComplete);
            sink.accept(ConnectionEvent.reconnection(data));
            MDC.remove("operation");
        }
    }

    /**
     * Resets the heartbeat timer and missed-beat counter without triggering recovery.
     * Also marks the handler as ready, enabling missed-beat counting.
     * <p>
     * Called by {@link com.pandascore.sdk.rmq.RabbitMQFeed} after a connection is
     * fully established. Until this is called, {@code checkHeartbeat()} is a no-op,
     * preventing false disconnection events during the initial AMQP dial-up.
     */
    public void resetTimer() {
        lastBeat = Instant.now();
        missedHeartbeats = 0;
        ready = true;
    }

    /**
     * Call on AMQP shutdown or any immediate disconnection to notify listeners.
     */
    public void handleDisconnection() {
        synchronized (stateLock) {
            if (!disconnected) {
                disconnected = true;
                downAt = Instant.now();
                missedHeartbeats = 0;
                MDC.put("operation", "disconnection");
                logger.warn("{}Disconnection detected", label());
                sink.accept(ConnectionEvent.disconnection());
                MDC.remove("operation");
            }
        }
    }

    // Internal check for missed heartbeats
    private void checkHeartbeat() {
        if (!ready) return;      // connection not yet established — do not count
        synchronized (stateLock) {
            if (disconnected) return;

            if (Duration.between(lastBeat, Instant.now()).compareTo(HEARTBEAT_INTERVAL.plus(HEARTBEAT_GRACE)) > 0) {
                missedHeartbeats++;
                logger.debug("{}Missed heartbeat #{}", label(), missedHeartbeats);
                if (missedHeartbeats >= MAX_MISSED_COUNT) {
                    disconnected = true;
                    downAt = Instant.now();
                    MDC.put("operation", "disconnection");
                    logger.warn("{}Missed {} heartbeats – marking disconnected", label(), missedHeartbeats);
                    sink.accept(ConnectionEvent.disconnection());
                    MDC.remove("operation");
                }
            } else {
                missedHeartbeats = 0;
            }
        }
    }

    /**
     * Gracefully stop heartbeat checks and shutdown the executor.
     */
    public void shutdown() {
        if (checkTask != null && !checkTask.isCancelled()) {
            checkTask.cancel(true);
        }
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Alias for shutdown to allow try-with-resources usage.
     */
    @Override
    public void close() {
        shutdown();
    }

    // Visible for testing
    boolean isDisconnected() {
        return disconnected;
    }

    // Visible for testing
    int getMissedHeartbeats() {
        return missedHeartbeats;
    }

    // Visible for testing
    boolean isReady() {
        return ready;
    }
}
