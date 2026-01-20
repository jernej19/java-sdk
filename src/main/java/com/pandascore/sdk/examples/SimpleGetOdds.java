package com.pandascore.sdk.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;
import com.pandascore.sdk.model.feed.markets.MarketsMessageMarket;
import com.pandascore.sdk.model.feed.markets.MarketsMessageSelection;
import com.pandascore.sdk.rmq.RabbitMQFeed;

/**
 * Simplest possible example: Connect to feed and print odds updates.
 *
 * USAGE:
 * 1. Replace YOUR_TOKEN, YOUR_COMPANY_ID, EMAIL, and PASSWORD below
 * 2. Run: java com.pandascore.sdk.examples.SimpleGetOdds
 * 3. Watch odds updates stream to console
 */
public class SimpleGetOdds {

    public static void main(String[] args) throws Exception {

        System.out.println("=== PandaScore Java SDK - Simple Odds Example ===\n");

        // ====================================================================
        // STEP 1: Configure SDK with your credentials
        // ====================================================================

        SDKOptions options = SDKOptions.builder()
            .apiToken("YOUR_TOKEN")           // Replace with your API token
            .companyId(12345)                 // Replace with your company ID
            .email("your-email@example.com")  // Replace with your email
            .password("your-password")        // Replace with your password
            .queueBinding(
                SDKOptions.QueueBinding.builder()
                    .queueName("my-odds-queue")
                    .routingKey("#")          // Subscribe to all messages
                    .build()
            )
            .americanOdds(true)               // Enable American odds format
            .build();

        SDKConfig.setOptions(options);
        System.out.println("✓ SDK configured for company ID: " + options.getCompanyId());

        // ====================================================================
        // STEP 2: Create event handler (handles disconnection/reconnection)
        // ====================================================================

        EventHandler eventHandler = new EventHandler(event -> {
            if ("disconnection".equals(event)) {
                System.out.println("\n⚠️  Disconnected from feed");
            } else if ("reconnection".equals(event)) {
                System.out.println("\n✓ Reconnected to feed");
            }
        });

        // ====================================================================
        // STEP 3: Create JSON parser
        // ====================================================================

        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

        // ====================================================================
        // STEP 4: Connect to feed and process odds
        // ====================================================================

        RabbitMQFeed feed = new RabbitMQFeed(eventHandler);

        feed.connect(message -> {
            JsonNode json = (JsonNode) message;
            String messageType = json.get("type").asText();

            // Only process markets (odds) messages
            if (!"markets".equals(messageType)) {
                return;
            }

            try {
                // Parse the markets message
                MarketsMessage marketsMsg = mapper.treeToValue(json, MarketsMessage.class);

                // Print the update
                printMarketsUpdate(marketsMsg);

            } catch (Exception e) {
                System.err.println("Error parsing markets message: " + e.getMessage());
            }
        });

        System.out.println("✓ Connected to feed!");
        System.out.println("\nListening for odds updates... (Press Ctrl+C to stop)\n");
        System.out.println("=".repeat(60));

        // Keep running indefinitely
        Thread.currentThread().join();
    }

    /**
     * Print a nice formatted markets update.
     */
    private static void printMarketsUpdate(MarketsMessage msg) {
        System.out.println("\n📊 ODDS UPDATE");
        System.out.println("   Time: " + msg.getAt());
        System.out.println("   Action: " + msg.getAction());
        System.out.println("   Match ID: " + msg.getMatchId());
        System.out.println("   Game: " + msg.getVideogameSlug());

        for (MarketsMessageMarket market : msg.getMarkets()) {
            System.out.println("\n   🎯 Market: " + market.getName());
            System.out.println("      Status: " + market.getStatus());
            System.out.println("      Template: " + market.getTemplate());

            if (market.getSelections() != null) {
                for (MarketsMessageSelection sel : market.getSelections()) {
                    String odds = formatOdds(sel);
                    System.out.printf("      ├─ %-30s %s%n", sel.getName(), odds);
                }
            }
        }

        System.out.println("-".repeat(60));
    }

    /**
     * Format odds nicely (shows decimal and American if available).
     */
    private static String formatOdds(MarketsMessageSelection sel) {
        StringBuilder sb = new StringBuilder();

        if (sel.getOddsDecimalWithOverround() != null) {
            sb.append(String.format("%.2f", sel.getOddsDecimalWithOverround()));
        }

        if (sel.getOddsAmericanWithOverround() != null) {
            double american = sel.getOddsAmericanWithOverround();
            String sign = american > 0 ? "+" : "";
            sb.append(String.format(" (%s%.0f)", sign, american));
        }

        if (sel.getProbabilityWithOverround() != null) {
            sb.append(String.format(" [%.1f%%]", sel.getProbabilityWithOverround() * 100));
        }

        return sb.toString();
    }
}
