package com.pandascore.sdk.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.pandascore.sdk.http.MatchesClient;

import java.time.Duration;
import java.time.Instant;
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
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(10);
    // Maximum duration without a heartbeat before marking disconnected
    private static final Duration MAX_MISSED = HEARTBEAT_INTERVAL.multipliedBy(3);

    private final ScheduledExecutorService scheduler;
    private final ScheduledFuture<?> checkTask;
    private final Consumer<String> sink;

    // Last time a heartbeat was received
    private volatile Instant lastBeat;
    // Whether currently marked as disconnected
    private volatile boolean disconnected;
    // Timestamp when disconnection was detected
    private volatile Instant downAt;

    /**
     * @param sink Consumer notified on "disconnection" or "reconnection" events
     */
    public EventHandler(Consumer<String> sink) {
        this.sink = sink;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "beat-watch");
            t.setDaemon(true);
            return t;
        });

        this.lastBeat = Instant.now();
        this.disconnected = false;
        this.downAt = null;

        // Schedule periodic check for missed heartbeats
        Map<String, String> savedMap = MDC.getCopyOfContextMap();
        this.checkTask = scheduler.scheduleAtFixedRate(
            () -> {
                MDC.setContextMap(savedMap);
                this.checkHeartbeat();
            },
            HEARTBEAT_INTERVAL.toMillis(),
            HEARTBEAT_INTERVAL.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Call when a heartbeat or health check is received to reset the timer.
     */
    public void heartbeat() {
        lastBeat = Instant.now();
        if (disconnected) {
            disconnected = false;
            Instant up = lastBeat;
            MDC.put("operation", "reconnection");
            logger.info("Reconnection detected");
            try {
                if (downAt != null) {
                    var recovered = MatchesClient.recoverMarkets(downAt.toString());
                    logger.info("Recovered {} matches with markets", recovered.size());
                    var matches = MatchesClient.fetchMatchesRange(downAt.toString(), up.toString());
                    logger.info("Recovered {} matches", matches.size());
                }
            } catch (Exception e) {
                logger.error("Automatic recovery failed", e);
            } finally {
                downAt = null;
            }
            sink.accept("reconnection");
            MDC.remove("operation");
        }
    }

    /**
     * Call on AMQP shutdown or any immediate disconnection to notify listeners.
     */
    public void handleDisconnection() {
        if (!disconnected) {
            disconnected = true;
            downAt = Instant.now();
            MDC.put("operation", "disconnection");
            logger.warn("Disconnection detected");
            sink.accept("disconnection");
            MDC.remove("operation");
        }
    }

    // Internal check for missed heartbeats
    private void checkHeartbeat() {
        if (!disconnected && Duration.between(lastBeat, Instant.now()).compareTo(MAX_MISSED) > 0) {
            disconnected = true;
            downAt = Instant.now();
            MDC.put("operation", "disconnection");
            logger.warn("Missed heartbeat – marking disconnected");
            sink.accept("disconnection");
            MDC.remove("operation");
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
}
