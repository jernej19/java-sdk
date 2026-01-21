package com.pandascore.sdk.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;
import com.pandascore.sdk.rmq.RabbitMQFeed;

/**
 * Example 1: Basic Odds Display
 *
 * Shows the simplest way to connect and display odds with American format.
 * Perfect for getting started quickly.
 *
 * Output Format:
 * - Market name and status
 * - Selection name with decimal and American odds
 * - Win probability percentage
 */
public class Example1_BasicOdds {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 1: Basic Odds Display ===\n");

        // Configure SDK
        SDKOptions options = SDKOptions.builder()
            .apiToken("YOUR_TOKEN")
            .companyId(12345)
            .email("your-email@example.com")
            .password("your-password")
            .queueBinding(
                SDKOptions.QueueBinding.builder()
                    .queueName("basic-odds-queue")
                    .routingKey("#")  // All messages (filter in code)
                    .build()
            )
            .americanOdds(true)
            .build();

        SDKConfig.setOptions(options);

        // Event handler
        EventHandler eventHandler = new EventHandler(event -> {
            System.out.println("[EVENT] " + event);
        });

        // JSON parser
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // Connect and display
        RabbitMQFeed feed = new RabbitMQFeed(eventHandler);
        feed.connect(message -> {
            JsonNode json = (JsonNode) message;

            if ("markets".equals(json.get("type").asText())) {
                try {
                    MarketsMessage msg = mapper.treeToValue(json, MarketsMessage.class);

                    System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    System.out.println("⚡ ODDS UPDATE");
                    System.out.println("   Match: " + msg.getMatchId());
                    System.out.println("   Game: " + msg.getVideogameSlug());
                    System.out.println("   Action: " + msg.getAction());
                    System.out.println("   Time: " + msg.getAt());

                    msg.getMarkets().forEach(market -> {
                        System.out.println("\n   📊 " + market.getName() + " [" + market.getStatus() + "]");

                        market.getSelections().forEach(sel -> {
                            Double decimal = sel.getOddsDecimalWithOverround();
                            Double american = sel.getOddsAmericanWithOverround();
                            Double prob = sel.getProbabilityWithOverround();

                            System.out.printf("      %-25s  %.2f  %7s  [%.1f%%]%n",
                                sel.getName(),
                                decimal != null ? decimal : 0.0,
                                formatAmerican(american),
                                prob != null ? prob * 100 : 0.0
                            );
                        });
                    });

                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        });

        System.out.println("✓ Connected! Streaming odds...\n");
        Thread.currentThread().join();
    }

    private static String formatAmerican(Double american) {
        if (american == null) return "   N/A";
        return String.format("%+6.0f", american);
    }
}
