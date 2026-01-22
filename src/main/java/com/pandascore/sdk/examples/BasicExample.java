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
 * Comprehensive example showing all message types:
 * - Markets (odds updates)
 * - Fixtures (match updates)
 * - Scoreboards (live scores)
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

        System.out.println("\n📊 MARKETS - Match #" + msg.getMatchId());
        System.out.println("   Action: " + msg.getAction());
        System.out.println("   Markets: " + msg.getMarkets().size());
        System.out.println("   Game: " + msg.getVideogameSlug());

        // Show first market name
        if (!msg.getMarkets().isEmpty()) {
            System.out.println("   First: " + msg.getMarkets().get(0).getName());
        }
    }

    private static void handleFixture(JsonNode json, ObjectMapper mapper) throws Exception {
        FixtureMessage msg = mapper.treeToValue(json, FixtureMessage.class);

        System.out.println("\n🎮 FIXTURE - " + msg.getAction().toString().toUpperCase());
        System.out.println("   Match ID: " + msg.getMatchId());
        System.out.println("   Event: " + msg.getEventType() + " #" + msg.getEventId());
        System.out.println("   Game: " + msg.getVideogameSlug());

        if (msg.getMatch() != null) {
            System.out.println("   Name: " + msg.getMatch().getName());
            System.out.println("   Status: " + msg.getMatch().getStatus());
        }
    }

    private static void handleScoreboard(JsonNode json) {
        System.out.println("\n🏆 SCOREBOARD");
        System.out.println("   Type: " + json.get("scoreboard_type").asText());
        System.out.println("   ID: " + json.get("id").asText());

        if (json.has("games")) {
            System.out.println("   Games: " + json.get("games").size());
        }
    }
}
