package com.pandascore.sdk.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;
import com.pandascore.sdk.model.feed.markets.MarketsMessageMarket;
import com.pandascore.sdk.rmq.RabbitMQFeed;

import java.util.Arrays;
import java.util.List;

/**
 * Example 3: Filter Specific Market Types
 *
 * Shows how to filter for specific market templates/types.
 * Useful when you only care about certain bet types (e.g., winner markets).
 *
 * This example filters for:
 * - winner-2-way
 * - winner-3-way
 * - correct-score
 *
 * Modify the MARKET_FILTERS list to track different markets.
 */
public class Example3_SpecificMarkets {

    // Market templates to filter for
    private static final List<String> MARKET_FILTERS = Arrays.asList(
        "winner-2-way",
        "winner-3-way",
        "correct-score"
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 3: Specific Market Types ===");
        System.out.println("Filtering for: " + String.join(", ", MARKET_FILTERS) + "\n");

        // Configure SDK
        SDKOptions options = SDKOptions.builder()
            .apiToken("YOUR_TOKEN")
            .companyId(12345)
            .email("your-email@example.com")
            .password("your-password")
            .queueBinding(
                SDKOptions.QueueBinding.builder()
                    .queueName("specific-markets-queue")
                    .routingKey("#")  // All messages (filter in code)
                    .build()
            )
            .americanOdds(true)
            .fractionalOdds(true)  // Enable both formats
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

                    // Filter markets by template
                    List<MarketsMessageMarket> filtered = msg.getMarkets().stream()
                        .filter(m -> MARKET_FILTERS.contains(m.getTemplate()))
                        .toList();

                    if (filtered.isEmpty()) {
                        return;  // Skip if no matching markets
                    }

                    System.out.println("\n" + "=".repeat(70));
                    System.out.println("📌 FILTERED MARKETS");
                    System.out.println("   Match: " + msg.getMatchId());
                    System.out.println("   Game:  " + msg.getVideogameSlug());
                    System.out.println("   Action: " + msg.getAction());
                    System.out.println("=".repeat(70));

                    filtered.forEach(market -> {
                        System.out.println("\n🎯 " + market.getName() + " [" + market.getTemplate() + "]");
                        System.out.println("   Status: " + market.getStatus());
                        System.out.println("   Overround: " + market.getOverround() + "%");

                        market.getSelections().forEach(sel -> {
                            System.out.printf("   %-20s │ %6.2f │ %7s │ %8s │ %.1f%%%n",
                                truncate(sel.getName(), 20),
                                sel.getOddsDecimalWithOverround(),
                                formatAmerican(sel.getOddsAmericanWithOverround()),
                                sel.getOddsFractionalWithOverround(),
                                sel.getProbabilityWithOverround() * 100
                            );
                        });
                    });

                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        });

        System.out.println("✓ Connected! Filtering markets...\n");
        Thread.currentThread().join();
    }

    private static String formatAmerican(Double american) {
        if (american == null) return "   N/A";
        return String.format("%+6.0f", american);
    }

    private static String truncate(String str, int length) {
        if (str == null) return "";
        return str.length() > length ? str.substring(0, length - 3) + "..." : str;
    }
}
