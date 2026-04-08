package com.pandascore.sdk.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.ConnectionEvent;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.rmq.RabbitMQFeed;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Multi-Connection Example
 *
 * Demonstrates creating multiple AMQP connections, each with its own
 * queue bindings. The SDK supports up to 10 concurrent connections,
 * with up to 10 queues per connection.
 *
 * <p><strong>Important:</strong> Each connection must receive heartbeat messages
 * to avoid being marked as disconnected after 30 seconds. Use {@code #} as the
 * routing key on at least one queue per connection, or ensure your routing keys
 * also match heartbeat messages.
 */
public class MultiConnectionExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== PandaScore SDK Multi-Connection Example ===\n");

        // 1. Configure global SDK settings (shared across all connections)
        SDKOptions options = SDKOptions.builder()
            .apiToken("YOUR_TOKEN")
            .companyId(12345)
            .email("your-email@example.com")
            .password("your-password")
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("default-queue")
                .routingKey("#")
                .build())
            .build();

        SDKConfig.setOptions(options);

        // 2. Create first connection — markets, WITH recovery enabled
        //    Only ONE connection should have recoverOnReconnect=true to avoid
        //    redundant recovery API calls across multiple connections.
        //
        //    Uses "#" to receive ALL messages (including heartbeats).
        //    The SDK filters heartbeats internally — your callback only
        //    receives business messages (markets, fixtures, scoreboards).
        List<SDKOptions.QueueBinding> marketsBindings = List.of(
            SDKOptions.QueueBinding.builder()
                .queueName("markets-queue")
                .routingKey("#")
                .build()
        );

        EventHandler marketsHandler = new EventHandler(event -> {
            logEvent("markets-feed", event);
        });

        RabbitMQFeed marketsFeed = new RabbitMQFeed(marketsHandler, marketsBindings, true);
        marketsFeed.connect(message -> {
            JsonNode json = (JsonNode) message;
            String type = json.has("type") ? json.get("type").asText() : "unknown";
            if ("markets".equals(type)) {
                System.out.println("[markets-feed] Match " + json.get("match_id")
                    + " — action: " + json.get("action"));
            }
        });

        // 3. Create second connection — fixtures and scoreboards, NO recovery
        //    Recovery is disabled on this connection since connection #1 handles it.
        //
        //    Also uses "#" to receive heartbeats. Filter by type in your callback.
        List<SDKOptions.QueueBinding> fixturesBindings = List.of(
            SDKOptions.QueueBinding.builder()
                .queueName("fixtures-queue")
                .routingKey("#")
                .build()
        );

        EventHandler fixturesHandler = new EventHandler(event -> {
            logEvent("fixtures-feed", event);
        });

        RabbitMQFeed fixturesFeed = new RabbitMQFeed(fixturesHandler, fixturesBindings, false);
        fixturesFeed.connect(message -> {
            JsonNode json = (JsonNode) message;
            String type = json.has("type") ? json.get("type").asText() : "unknown";
            if ("fixture".equals(type) || "scoreboard".equals(type)) {
                System.out.println("[fixtures-feed] " + type + " — Match " + json.get("match_id"));
            }
        });

        System.out.println("Active connections: " + RabbitMQFeed.getActiveConnectionCount());
        System.out.println("Connected! Monitoring markets on one connection, fixtures+scoreboards on another...\n");

        // Keep alive
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            marketsFeed.close();
            fixturesFeed.close();
        }));

        Thread.currentThread().join();
    }

    private static void logEvent(String feedName, ConnectionEvent event) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (event.getCode() == ConnectionEvent.CODE_DISCONNECTION) {
            System.out.println("[" + time + "] [" + feedName + "] DISCONNECTION");
        } else {
            ConnectionEvent.RecoveryData data = event.getRecoveryData();
            System.out.println("[" + time + "] [" + feedName + "] RECONNECTION — recovered "
                + data.getMarkets().size() + " markets, " + data.getMatches().size() + " matches"
                + (data.isComplete() ? "" : " (partial)"));
        }
    }
}
