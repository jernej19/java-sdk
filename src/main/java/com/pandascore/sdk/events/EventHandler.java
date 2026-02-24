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
    // Number of consecutive missed heartbeats before triggering disconnection
    static final int MAX_MISSED_COUNT = 3;

    private final ScheduledExecutorService scheduler;
    private final ScheduledFuture<?> checkTask;
    private final Consumer<ConnectionEvent> sink;

    // Last time a heartbeat was received
    private volatile Instant lastBeat;
    // Number of consecutive missed heartbeats
    private volatile int missedHeartbeats;
    // Whether currently marked as disconnected
    private volatile boolean disconnected;
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
     * Call when a heartbeat message is received to reset the timer and missed-beat counter.
     * If previously disconnected, initiates recovery process before notifying application.
     */
    public void heartbeat() {
        lastBeat = Instant.now();
        missedHeartbeats = 0;
        if (disconnected) {
            disconnected = false;
            Instant up = lastBeat;
            MDC.put("operation", "reconnect");
            logger.info("Heartbeat restored - starting recovery");

            // Start buffering messages during recovery
            if (feed != null) {
                feed.startRecovery();
            }

            List<MarketsRecoveryMatch> recoveredMarkets = Collections.emptyList();
            List<FixtureMatch> recoveredMatches = Collections.emptyList();

            try {
                boolean recover = SDKConfig.getInstance().getOptions().isRecoverOnReconnect();
                if (recover && downAt != null) {
                    recoveredMarkets = MatchesClient.recoverMarkets(downAt.toString());
                    recoveredMatches = MatchesClient.fetchMatchesRange(downAt.toString(), up.toString());
                }
                logger.info("Recovery complete - reconnection successful");
            } catch (Exception e) {
                logger.error("Automatic recovery failed", e);
            } finally {
                downAt = null;
            }

            // End recovery: process buffered messages then resume normal operation
            if (feed != null) {
                feed.endRecovery();
            }

            // Notify application AFTER recovery is complete AND buffered messages are processed
            ConnectionEvent.RecoveryData data = new ConnectionEvent.RecoveryData(recoveredMarkets, recoveredMatches);
            sink.accept(ConnectionEvent.reconnection(data));
            MDC.remove("operation");
        }
    }

    /**
     * Resets the heartbeat timer and missed-beat counter without triggering recovery.
     * Call this after a transport-level AMQP reconnection to prevent the timer from
     * accumulating stale missed beats from before the reconnection.
     */
    public void resetTimer() {
        lastBeat = Instant.now();
        missedHeartbeats = 0;
    }

    /**
     * Call on AMQP shutdown or any immediate disconnection to notify listeners.
     */
    public void handleDisconnection() {
        if (!disconnected) {
            disconnected = true;
            downAt = Instant.now();
            missedHeartbeats = 0;
            MDC.put("operation", "disconnection");
            logger.info("Disconnection detected");
            sink.accept(ConnectionEvent.disconnection());
            MDC.remove("operation");
        }
    }

    // Internal check for missed heartbeats
    private void checkHeartbeat() {
        if (disconnected) return;

        if (Duration.between(lastBeat, Instant.now()).compareTo(HEARTBEAT_INTERVAL) > 0) {
            missedHeartbeats++;
            logger.debug("Missed heartbeat #{}", missedHeartbeats);
            if (missedHeartbeats >= MAX_MISSED_COUNT) {
                disconnected = true;
                downAt = Instant.now();
                MDC.put("operation", "disconnection");
                logger.info("Missed {} heartbeats – marking disconnected", missedHeartbeats);
                sink.accept(ConnectionEvent.disconnection());
                MDC.remove("operation");
            }
        } else {
            missedHeartbeats = 0;
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
}
