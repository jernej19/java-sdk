package com.pandascore.sdk.config;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import java.util.List;
import java.util.Objects;

/**
 * SDKOptions holds configuration settings for the PandaScore SDK.
 * <p>
 * Use the SDKOptions builder to create an instance. All required fields must be set before calling {@link SDKConfig#setOptions(SDKOptions)}.
 */
@Data
@Builder
public class SDKOptions {
    /**
     * API token for authenticating REST requests.
     */
    private final String apiToken;

    /**
     * Numeric identifier of your PandaScore company/account.
     */
    private final long companyId;

    /**
     * Email associated with your PandaScore account.
     */
    private final String email;

    /**
     * Password for your PandaScore account.
     */
    private final String password;

    /**
     * Hostname for the RabbitMQ feed.
     */
    @Builder.Default
    private final String feedHost = "trading-feed.pandascore.co";

    /**
     * Base URL for the REST API used for recovery.
     */
    @Builder.Default
    private final String apiBaseUrl = "https://api.pandascore.co/betting/matches";

    /**
     * List of RabbitMQ queue bindings to declare on the feed exchange.
     */
    @Singular
    private final List<QueueBinding> queueBindings;

    /**
     * If true, log every message payload at INFO level; otherwise payloads are logged at DEBUG.
     */
    @Builder.Default
    private final boolean alwaysLogPayload = false;

    /**
     * If true, compute American odds for each selection and expose them via
     * {@code oddsAmerican} and {@code oddsAmericanWithOverround} fields.
     */
    @Builder.Default
    private final boolean americanOdds = false;

    /**
     * If true, compute fractional odds for each selection and expose them via
     * {@code oddsFractional} and {@code oddsFractionalWithOverround} fields.
     */
    @Builder.Default
    private final boolean fractionalOdds = false;

    /**
     * Maximum number of unacknowledged messages the broker will push to each consumer.
     * Default: 1 (strict ordering guarantee).
     * <p>
     * With prefetch=1, each consumer processes one message at a time, ensuring
     * messages are handled in the order they arrive. Higher values improve throughput
     * but risk out-of-order processing when multiple consumers share the same queue.
     * <p>
     * Set to 0 for unlimited prefetch (not recommended for production).
     */
    @Builder.Default
    private final int prefetchCount = 1;

    /**
     * Whether to automatically trigger recovery (recoverMarkets + fetchMatchesRange)
     * when reconnection occurs. Default: true.
     * <p>
     * If false, SDK emits reconnection event but does NOT call recovery APIs.
     * User must manually call MatchesClient methods if they want recovery.
     */
    @Builder.Default
    private final boolean recoverOnReconnect = true;

    /**
     * Maximum number of queue bindings allowed per connection.
     */
    public static final int MAX_QUEUES_PER_CONNECTION = 10;

    /**
     * Represents a single RabbitMQ queue + routing-key pair.
     */
    @Data
    @Builder
    public static class QueueBinding {
        /** Name of the RabbitMQ queue to declare. */
        private final String queueName;
        /** Routing key for binding the queue to the feed exchange. */
        private final String routingKey;

        /**
         * Validate that neither queueName nor routingKey is null.
         * This method can be called during build time.
         */
        public void validate() {
            Objects.requireNonNull(queueName, "queueName must not be null");
            Objects.requireNonNull(routingKey, "routingKey must not be null");
        }
    }

    /**
     * Validates that required fields are non-null and queueBindings is non-empty.
     * This is automatically invoked when setting options in {@link SDKConfig}.
     */
    public void validate() {
        Objects.requireNonNull(apiToken, "apiToken must not be null");
        if (companyId <= 0) {
            throw new IllegalArgumentException("companyId must be positive");
        }
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(feedHost, "feedHost must not be null");
        Objects.requireNonNull(apiBaseUrl, "apiBaseUrl must not be null");
        Objects.requireNonNull(queueBindings, "queueBindings must not be null");
        if (queueBindings.isEmpty()) {
            throw new IllegalArgumentException("queueBindings must not be empty");
        }
        if (queueBindings.size() > MAX_QUEUES_PER_CONNECTION) {
            throw new IllegalArgumentException(
                "Cannot bind more than " + MAX_QUEUES_PER_CONNECTION
                    + " queues per connection. Specified: " + queueBindings.size());
        }
        // Validate each QueueBinding
        queueBindings.forEach(QueueBinding::validate);
    }
}
