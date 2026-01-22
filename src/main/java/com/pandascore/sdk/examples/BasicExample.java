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

        System.out.printf("MARKETS: matchId=%s action=%s markets=%d game=%s%n",
            msg.getMatchId() != null ? msg.getMatchId() : "N/A",
            msg.getAction(),
            msg.getMarkets().size(),
            msg.getVideogameSlug()
        );
    }

    private static void handleFixture(JsonNode json, ObjectMapper mapper) throws Exception {
        FixtureMessage msg = mapper.treeToValue(json, FixtureMessage.class);

        // Determine Match ID based on event type
        Long matchId = "match".equals(msg.getEventType()) ? msg.getEventId() : msg.getMatchId();

        String name = msg.getMatch() != null ? msg.getMatch().getName() : "N/A";
        String status = msg.getMatch() != null ? msg.getMatch().getStatus() : "N/A";

        if ("game".equals(msg.getEventType())) {
            System.out.printf("FIXTURE: matchId=%s eventType=%s gameId=%s action=%s game=%s name=\"%s\" status=%s%n",
                matchId,
                msg.getEventType(),
                msg.getEventId(),
                msg.getAction(),
                msg.getVideogameSlug(),
                name,
                status
            );
        } else {
            System.out.printf("FIXTURE: matchId=%s eventType=%s action=%s game=%s name=\"%s\" status=%s%n",
                matchId,
                msg.getEventType(),
                msg.getAction(),
                msg.getVideogameSlug(),
                name,
                status
            );
        }
    }

    private static void handleScoreboard(JsonNode json) {
        String type = json.get("scoreboard_type").asText();
        String id = json.get("id").asText();
        int games = json.has("games") ? json.get("games").size() : 0;

        System.out.printf("SCOREBOARD: type=%s id=%s games=%d%n", type, id, games);
    }
}
