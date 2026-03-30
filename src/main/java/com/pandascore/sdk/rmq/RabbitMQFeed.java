package com.pandascore.sdk.rmq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.EventHandler;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * RabbitMQFeed handles connecting to the PandaScore feed over AMQPS,
 * consuming messages, automatic reconnection, and clean shutdown.
 * <p>
 * Implements AutoCloseable for use in try-with-resources.
 */
public final class RabbitMQFeed implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQFeed.class);

    /**
     * Maximum number of concurrent AMQP connections before the SDK emits a warning.
     * Exceeding this limit usually indicates a misuse pattern (e.g. creating new
     * RabbitMQFeed instances without closing previous ones).
     */
    static final int MAX_CONNECTIONS = 10;

    /** Global counter of active AMQP connections across all RabbitMQFeed instances. */
    private static final AtomicInteger activeConnections = new AtomicInteger(0);

    private final SDKOptions opts = SDKConfig.getInstance().getOptions();
    private final boolean alwaysLogPayload = opts.isAlwaysLogPayload();
    private final EventHandler handler;
    private Connection conn;
    private Channel chan;
    private final ObjectMapper mapper = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(new JavaTimeModule());
    private final ScheduledExecutorService retry = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rmq-retry");
        t.setDaemon(true);
        return t;
    });
    private int attempt = 1;

    // Per-connection queue bindings (if provided, overrides global config)
    private final List<SDKOptions.QueueBinding> queueBindings;

    // Recovery state: when true, buffer messages instead of processing them
    private volatile boolean recovering = false;
    // Buffer for messages received during recovery
    private final Queue<JsonNode> recoveryBuffer = new ConcurrentLinkedQueue<>();
    // Customer sink reference (needed for processing buffered messages)
    private volatile Consumer<Object> customerSink;

    /**
     * Constructs a new RabbitMQFeed with the given EventHandler.
     * Uses queue bindings from the global SDKConfig.
     *
     * @param handler handles heartbeat and disconnection events
     */
    public RabbitMQFeed(EventHandler handler) {
        this(handler, null);
    }

    /**
     * Constructs a new RabbitMQFeed with the given EventHandler and
     * per-connection queue bindings.
     *
     * @param handler        handles heartbeat and disconnection events
     * @param queueBindings  queue bindings for this connection, or null to use global config
     * @throws IllegalArgumentException if queueBindings is empty or exceeds {@link SDKOptions#MAX_QUEUES_PER_CONNECTION}
     */
    public RabbitMQFeed(EventHandler handler, List<SDKOptions.QueueBinding> queueBindings) {
        this.handler = handler;
        if (queueBindings != null) {
            if (queueBindings.isEmpty()) {
                throw new IllegalArgumentException("queueBindings must not be empty");
            }
            if (queueBindings.size() > SDKOptions.MAX_QUEUES_PER_CONNECTION) {
                throw new IllegalArgumentException(
                    "Cannot bind more than " + SDKOptions.MAX_QUEUES_PER_CONNECTION
                        + " queues per connection. Specified: " + queueBindings.size());
            }
            queueBindings.forEach(SDKOptions.QueueBinding::validate);
            this.queueBindings = Collections.unmodifiableList(queueBindings);
        } else {
            this.queueBindings = null;
        }
    }

    /**
     * Returns the effective queue bindings for this connection.
     * Uses per-connection bindings if provided, otherwise falls back to global config.
     */
    private List<SDKOptions.QueueBinding> getEffectiveQueueBindings() {
        return queueBindings != null ? queueBindings : opts.getQueueBindings();
    }

    /**
     * Connects to the RabbitMQ feed and starts consuming messages.
     * Automatically retries on failure with exponential backoff up to 60s.
     *
     * @param sink consumer to process each incoming JSON event
     */
    public void connect(Consumer<Object> sink) {
        this.customerSink = sink;  // Store reference for buffer processing

        // Wire up feed reference to handler for recovery control
        handler.setFeed(this);

        // Preserve tracing context
        MDC.put("session", MDC.get("session"));
        MDC.put("customerId", String.valueOf(opts.getCompanyId()));
        MDC.put("feed", "allOddsFeed");
        MDC.put("operation", "connect");

        logger.info("Connecting to feed host (attempt #{})", attempt);
        try {
            establish();
            startConsumers(sink);
            // Reset the heartbeat timer after transport reconnection without
            // triggering recovery — recovery should only start when a real
            // heartbeat message arrives from the server.
            handler.resetTimer();
            logger.info("Connected successfully");
            attempt = 1;
        } catch (Exception e) {
            logger.error("Connection failed on attempt #{}", attempt, e);
            scheduleReconnect(sink);
        } finally {
            MDC.remove("operation");
        }
    }

    /**
     * Establishes the AMQPS connection and declares exchange and queues.
     * Closes any existing connection before creating a new one to prevent leaks.
     *
     * @throws Exception on connection or SSL setup failures
     */
    private void establish() throws Exception {
        // Close any existing connection to prevent leaks (e.g. connect() called twice)
        closeExistingConnection();

        String host = opts.getFeedHost();
        int companyId = (int) opts.getCompanyId();
        String user = opts.getEmail();
        String pass = opts.getPassword();
        String vhost = URLEncoder.encode("odds/" + companyId, StandardCharsets.UTF_8);
        String uri = String.format(
            "amqps://%s:%s@%s/%s",
            URLEncoder.encode(user, StandardCharsets.UTF_8),
            URLEncoder.encode(pass, StandardCharsets.UTF_8),
            host,
            vhost
        );

        ConnectionFactory factory = new ConnectionFactory();
        factory.setRequestedHeartbeat(10);
        factory.setAutomaticRecoveryEnabled(false);

        // Use default JVM truststore for TLS
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, tmf.getTrustManagers(), null);
        factory.useSslProtocol(sslContext);

        factory.setUri(uri);
        conn = factory.newConnection();
        chan = conn.createChannel();

        // Set QoS prefetch to limit unacked messages per consumer.
        // Without this, RabbitMQ pushes unlimited messages causing large Unacked counts.
        int prefetch = opts.getPrefetchCount();
        if (prefetch > 0) {
            chan.basicQos(prefetch);
            logger.info("Channel QoS prefetch set to {}", prefetch);
        } else {
            logger.warn("Channel QoS prefetch is unlimited (0) — not recommended for production");
        }

        int count = activeConnections.incrementAndGet();
        if (count > MAX_CONNECTIONS) {
            activeConnections.decrementAndGet();
            conn.close();
            conn = null;
            chan = null;
            throw new IllegalStateException(
                "Cannot create more than " + MAX_CONNECTIONS
                    + " concurrent AMQP connections. Currently active: " + (count - 1)
                    + ". Call close() on unused feeds to release connections.");
        }
        logger.info("AMQP connection established (active connections: {})", count);

        // Listen for shutdown to trigger disconnection handling
        conn.addShutdownListener(cause -> {
            MDC.put("operation", "shutdown");
            int remaining = activeConnections.decrementAndGet();
            logger.warn("AMQP shutdown: {} (active connections: {})", cause.getMessage(), remaining);
            handler.handleDisconnection();
            MDC.remove("operation");
        });

        // Declare exchange and queues
        String exchange = "pandascore.feed";
        chan.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true);
        for (SDKOptions.QueueBinding qb : getEffectiveQueueBindings()) {
            MDC.put("routingKey", qb.getRoutingKey());
            chan.queueDeclare(qb.getQueueName(), true, false, false, null);
            chan.queueBind(qb.getQueueName(), exchange, qb.getRoutingKey());
            logger.info("Declared queue {} bound to {}", qb.getQueueName(), qb.getRoutingKey());
            MDC.remove("routingKey");
        }
    }

    /**
     * Starts consumers on all configured queues, dispatching messages to the sink.
     * The {@code feed} MDC tag is updated to the event type for each message
     * while {@code messageType} holds the raw {@code type} field from the JSON.
     *
     * @param sink consumer callback for business events
     * @throws IOException on consumer setup errors
     */
    private void startConsumers(Consumer<Object> sink) throws IOException {
        Map<String, String> savedMap = MDC.getCopyOfContextMap();
        DeliverCallback cb = (consumerTag, msg) -> {
            MDC.setContextMap(savedMap);
            String rk = msg.getEnvelope().getRoutingKey();
            MDC.put("routingKey", rk);

            JsonNode json = mapper.readTree(msg.getBody());
            String type = json.has("type") ? json.get("type").asText() : rk;
            MDC.put("messageType", type);
            MDC.put("feed", type); // show event type in 'feed' tag

            // Heartbeat detection: has "at" field but no "type" field
            // (matches TypeScript SDK convention: message.at && !message.type)
            boolean isHeartbeat = isHeartbeatMessage(json);
            if (isHeartbeat) {
                handler.heartbeat();
                logger.debug("Received heartbeat");
                chan.basicAck(msg.getEnvelope().getDeliveryTag(), false);
            } else {
                // Business event processing
                String[] parts = rk.split("\\.");
                String eventType = parts[parts.length - 4];
                String eventId   = parts[parts.length - 3];
                String action    = parts[parts.length - 1];

                // Check if we're in recovery mode
                if (recovering) {
                    // Buffer message for later processing
                    recoveryBuffer.add(json);
                    logger.debug("Buffered message during recovery: type={} eventType={} eventId={} action={}",
                        type, eventType, eventId, action);
                    chan.basicAck(msg.getEnvelope().getDeliveryTag(), false);
                } else {
                    // Normal processing
                    logger.info("Event: type={} eventType={} eventId={} action={}",
                        type, eventType, eventId, action
                    );
                    if (alwaysLogPayload) {
                        logger.info("Payload for eventType={} eventId={}: {}",
                            eventType, eventId, json.toString()
                        );
                    } else {
                        logger.debug("Payload for eventType={} eventId={}: {}",
                            eventType, eventId, json.toString()
                        );
                    }

                    try {
                        sink.accept(json);
                        chan.basicAck(msg.getEnvelope().getDeliveryTag(), false);
                    } catch (Exception e) {
                        logger.error("Error processing eventId={} action={}",
                            eventId, action, e);
                        chan.basicNack(msg.getEnvelope().getDeliveryTag(), false, true);
                    }
                }
            }
            MDC.remove("routingKey");
            MDC.remove("messageType");
            MDC.remove("feed");
        };

        for (SDKOptions.QueueBinding qb : getEffectiveQueueBindings()) {
            chan.basicConsume(
                qb.getQueueName(),
                false,
                qb.getQueueName() + "_consumer",
                cb,
                consumerTag -> {}
            );
        }
    }

    /**
     * Schedules a reconnection attempt with exponential backoff.
     *
     * @param sink the same consumer passed to connect()
     */
    private void scheduleReconnect(Consumer<Object> sink) {
        int delay = Math.min(attempt * 5, 60);
        MDC.put("operation", "reconnect");
        logger.warn("Reconnecting in {}s (attempt #{})", delay, attempt);
        Map<String, String> savedMap = MDC.getCopyOfContextMap();
        retry.schedule(() -> {
            MDC.setContextMap(savedMap);
            attempt++;
            connect(sink);
        }, delay, TimeUnit.SECONDS);
        MDC.remove("operation");
    }

    /**
     * Starts recovery mode: buffers incoming messages instead of processing them.
     * Called by EventHandler when recovery begins.
     */
    public void startRecovery() {
        recovering = true;
        recoveryBuffer.clear();  // Clear any stale buffered messages
        logger.info("Recovery mode started - buffering messages");
    }

    /**
     * Ends recovery mode: processes buffered messages then resumes normal operation.
     * Called by EventHandler when recovery completes.
     */
    public void endRecovery() {
        logger.info("Recovery mode ending - processing {} buffered messages", recoveryBuffer.size());

        // Process all buffered messages
        JsonNode bufferedMessage;
        int processed = 0;
        while ((bufferedMessage = recoveryBuffer.poll()) != null) {
            try {
                if (customerSink != null) {
                    customerSink.accept(bufferedMessage);
                    processed++;
                }
            } catch (Exception e) {
                logger.error("Error processing buffered message", e);
            }
        }

        logger.info("Processed {} buffered messages - resuming normal operation", processed);
        recovering = false;
    }

    /**
     * Closes the existing AMQP channel and connection if they are open.
     * Decrements the global active connection counter.
     * Called before establishing a new connection to prevent leaks.
     */
    private void closeExistingConnection() {
        try {
            if (chan != null && chan.isOpen()) {
                chan.close();
            }
        } catch (Exception e) {
            logger.debug("Error closing existing channel", e);
        }
        try {
            if (conn != null && conn.isOpen()) {
                conn.close();
                activeConnections.decrementAndGet();
            }
        } catch (Exception e) {
            logger.debug("Error closing existing connection", e);
        }
        chan = null;
        conn = null;
    }

    /**
     * Cleanly shuts down the feed: stops heartbeats, cancels retries,
     * and closes channels and connection.
     */
    @Override
    public void close() {
        retry.shutdownNow();
        try {
            handler.shutdown();
        } catch (Exception e) {
            logger.warn("Error shutting down event handler", e);
        }
        closeExistingConnection();
    }

    /**
     * Returns the current number of active AMQP connections across all RabbitMQFeed instances.
     *
     * @return the active connection count
     */
    public static int getActiveConnectionCount() {
        return activeConnections.get();
    }

    // Visible for testing: reset the global counter
    static void resetActiveConnectionCount() {
        activeConnections.set(0);
    }

    /**
     * Determines if a JSON message is a heartbeat.
     * Heartbeats have an "at" field but no "type" field, matching the
     * TypeScript SDK convention: {@code message.at && !message.type}.
     *
     * @param json the parsed JSON message
     * @return true if the message is a heartbeat
     */
    static boolean isHeartbeatMessage(JsonNode json) {
        return json.has("at") && !json.has("type");
    }
}
