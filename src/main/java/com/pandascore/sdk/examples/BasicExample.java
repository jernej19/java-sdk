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
 *
 * Shows message statistics and provides summary view of activity.
 */
public class BasicExample {

    private static int marketsCount = 0;
    private static int fixturesCount = 0;
    private static int scoreboardsCount = 0;
    private static int otherCount = 0;

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

        // Stats printer thread (every 10 seconds)
        Thread statsThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                    printStats();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statsThread.setDaemon(true);
        statsThread.start();

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
                    default -> {
                        otherCount++;
                        System.out.println("❓ Unknown type: " + type);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing " + type + ": " + e.getMessage());
            }
        });

        System.out.println("✓ Connected! Monitoring all message types...");
        System.out.println("  Stats will be printed every 10 seconds.\n");

        Thread.currentThread().join();
    }

    private static void handleMarkets(JsonNode json, ObjectMapper mapper) throws Exception {
        marketsCount++;
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
        fixturesCount++;
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
        scoreboardsCount++;

        System.out.println("\n🏆 SCOREBOARD");
        System.out.println("   Type: " + json.get("scoreboard_type").asText());
        System.out.println("   ID: " + json.get("id").asText());

        if (json.has("games")) {
            System.out.println("   Games: " + json.get("games").size());
        }
    }

    private static void printStats() {
        System.out.println("\n" + "━".repeat(50));
        System.out.println("📈 MESSAGE STATISTICS");
        System.out.println("━".repeat(50));
        System.out.printf("   Markets:     %6d%n", marketsCount);
        System.out.printf("   Fixtures:    %6d%n", fixturesCount);
        System.out.printf("   Scoreboards: %6d%n", scoreboardsCount);
        System.out.printf("   Other:       %6d%n", otherCount);
        System.out.printf("   TOTAL:       %6d%n", marketsCount + fixturesCount + scoreboardsCount + otherCount);
        System.out.println("━".repeat(50) + "\n");
    }
}
