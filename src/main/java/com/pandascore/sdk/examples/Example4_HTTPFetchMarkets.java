package com.pandascore.sdk.examples;

import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.http.MatchesClient;
import com.pandascore.sdk.model.feed.fixtures.FixtureMatch;
import com.pandascore.sdk.model.feed.markets.MarketsMessageMarket;

import java.util.List;

/**
 * Example 4: HTTP API - Fetch Markets
 *
 * Uses HTTP endpoints instead of streaming feed.
 * Perfect for:
 * - One-time data fetches
 * - Building snapshot views
 * - Getting initial state before streaming
 *
 * Methods demonstrated:
 * - fetchMatch(id): Get match details
 * - fetchMarkets(matchId): Get all markets for a match
 */
public class Example4_HTTPFetchMarkets {

    public static void main(String[] args) {
        System.out.println("=== Example 4: HTTP API - Fetch Markets ===\n");

        try {
            // Configure SDK
            SDKOptions options = SDKOptions.builder()
                .apiToken("YOUR_TOKEN")
                .companyId(12345)
                .email("your-email@example.com")
                .password("your-password")
                .queueBinding(
                    SDKOptions.QueueBinding.builder()
                        .queueName("dummy")  // Required but not used for HTTP
                        .routingKey("#")
                        .build()
                )
                .americanOdds(true)
                .build();

            SDKConfig.setOptions(options);

            // ========================================
            // Example 1: Fetch single match details
            // ========================================
            System.out.println("📋 Fetching match details...\n");

            String matchId = "123456";  // Replace with real match ID
            FixtureMatch match = MatchesClient.fetchMatch(matchId);

            System.out.println("Match: " + match.getName());
            System.out.println("Status: " + match.getStatus());
            System.out.println("Scheduled: " + match.getScheduledAt());

            if (match.getLeague() != null) {
                System.out.println("League: " + match.getLeague().getName());
            }

            if (match.getOpponents() != null) {
                System.out.println("\nTeams:");
                match.getOpponents().forEach(opp -> {
                    if (opp.getOpponent() != null) {
                        System.out.println("  • " + opp.getOpponent().getName());
                    }
                });
            }

            // ========================================
            // Example 2: Fetch markets for match
            // ========================================
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 Fetching markets for match " + matchId + "...\n");

            List<MarketsMessageMarket> markets = MatchesClient.fetchMarkets(matchId);

            System.out.println("Found " + markets.size() + " markets\n");

            // Display first 5 markets
            markets.stream()
                .limit(5)
                .forEach(market -> {
                    System.out.println("\n🎯 " + market.getName());
                    System.out.println("   Template: " + market.getTemplate());
                    System.out.println("   Status: " + market.getStatus());
                    System.out.println("   Selections:");

                    market.getSelections().forEach(sel -> {
                        System.out.printf("      %-25s  %.2f  (%s)  [%.1f%%]%n",
                            sel.getName(),
                            sel.getOddsDecimalWithOverround(),
                            formatAmerican(sel.getOddsAmericanWithOverround()),
                            sel.getProbabilityWithOverround() * 100
                        );
                    });
                });

            if (markets.size() > 5) {
                System.out.println("\n... and " + (markets.size() - 5) + " more markets");
            }

            System.out.println("\n✓ Done!");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String formatAmerican(Double american) {
        if (american == null) return "N/A";
        return (american > 0 ? "+" : "") + String.format("%.0f", american);
    }
}
