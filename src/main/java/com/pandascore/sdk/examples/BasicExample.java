package com.pandascore.sdk.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.model.feed.fixtures.FixtureMessage;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;
import com.pandascore.sdk.rmq.RabbitMQFeed;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Basic Example: All Message Types
 *
 * Comprehensive example showing all message types with programmatic output:
 * - Markets (odds updates)
 * - Fixtures (match updates)
 * - Scoreboards (live scores)
 * - Disconnection/reconnection events
 */
public class BasicExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== PandaScore SDK Basic Example ===\n");

        // Configure SDK
        SDKOptions options = SDKOptions.builder()
            .apiToken("YOUR_TOKEN")
            .companyId(12345)
            .email("your-email@example.com")
            .password("your-password")
            .queueBinding(
                SDKOptions.QueueBinding.builder()
                    .queueName("your-queue-name")
                    .routingKey("#")  // Subscribe to ALL messages
                    .build()
            )
            .build();

        SDKConfig.setOptions(options);

        // Event handler for disconnection/reconnection
        EventHandler eventHandler = new EventHandler(event -> {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.println("[" + time + "] 🔔 " + event.toUpperCase());
        });

        // JSON parser
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // Connect and display
        RabbitMQFeed feed = new RabbitMQFeed(eventHandler);
        feed.connect(message -> {
            JsonNode json = (JsonNode) message;
            String type = json.get("type").asText();

            try {
                switch (type) {
                    case "markets" -> handleMarkets(json, mapper);
                    case "fixture" -> handleFixture(json, mapper);
                    case "scoreboard" -> handleScoreboard(json);
                    default -> System.out.println("❓ Unknown type: " + type);
                }
            } catch (Exception e) {
                System.err.println("Error processing " + type + ": " + e.getMessage());
            }
        });

        System.out.println("✓ Connected! Monitoring all message types...\n");

        Thread.currentThread().join();
    }

    private static void handleMarkets(JsonNode json, ObjectMapper mapper) throws Exception {
        MarketsMessage msg = mapper.treeToValue(json, MarketsMessage.class);

        System.out.printf("type=markets eventType=%s eventId=%s action=%s%n",
            msg.getEventType(),
            msg.getEventId(),
            msg.getAction()
        );
    }

    private static void handleFixture(JsonNode json, ObjectMapper mapper) throws Exception {
        FixtureMessage msg = mapper.treeToValue(json, FixtureMessage.class);

        System.out.printf("type=fixture eventType=%s eventId=%s action=%s%n",
            msg.getEventType(),
            msg.getEventId(),
            msg.getAction()
        );
    }

    private static void handleScoreboard(JsonNode json) {
        String scoreboardType = json.get("scoreboard_type").asText();
        String id = json.get("id").asText();

        System.out.printf("type=scoreboard scoreboardType=%s id=%s%n", scoreboardType, id);
    }
}
