package com.pandascore.sdk.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.model.feed.fixtures.FixtureMessage;
import com.pandascore.sdk.model.feed.fixtures.FixtureMatch;
import com.pandascore.sdk.rmq.RabbitMQFeed;

/**
 * Example 2: Fixture/Match Updates
 *
 * Monitors match status changes (started, finished, settled, etc.)
 * Displays full match details including teams, league, and tournament.
 *
 * Output Format:
 * - Match name and status
 * - Scheduled time
 * - Teams/opponents
 * - League and tournament info
 * - Winner (if decided)
 */
public class Example2_FixtureUpdates {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 2: Fixture/Match Updates ===\n");

        // Configure SDK
        SDKOptions options = SDKOptions.builder()
            .apiToken("YOUR_TOKEN")
            .companyId(12345)
            .email("your-email@example.com")
            .password("your-password")
            .queueBinding(
                SDKOptions.QueueBinding.builder()
                    .queueName("fixture-updates-queue")
                    .routingKey("pandascore.fixtures.#")  // Only fixtures
                    .build()
            )
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

            if ("fixture".equals(json.get("type").asText())) {
                try {
                    FixtureMessage msg = mapper.treeToValue(json, FixtureMessage.class);
                    FixtureMatch match = msg.getMatch();

                    if (match == null) return;

                    System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    System.out.println("🎮 FIXTURE UPDATE: " + msg.getAction().toString().toUpperCase());
                    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    System.out.println("Match:       " + match.getName());
                    System.out.println("ID:          " + match.getId());
                    System.out.println("Status:      " + match.getStatus());
                    System.out.println("Game:        " + msg.getVideogameSlug());
                    System.out.println("Scheduled:   " + match.getScheduledAt());

                    if (match.getBeginAt() != null) {
                        System.out.println("Started:     " + match.getBeginAt());
                    }

                    // League/Tournament info
                    if (match.getLeague() != null) {
                        System.out.println("\nLeague:      " + match.getLeague().getName());
                    }
                    if (match.getTournament() != null) {
                        System.out.println("Tournament:  " + match.getTournament().getName());
                        System.out.println("Tier:        " + match.getTournament().getTier());
                    }

                    // Opponents
                    if (match.getOpponents() != null && !match.getOpponents().isEmpty()) {
                        System.out.println("\n👥 Opponents:");
                        match.getOpponents().forEach(opp -> {
                            if (opp.getOpponent() != null) {
                                System.out.println("   • " + opp.getOpponent().getName() +
                                    " (" + opp.getOpponent().getAcronym() + ")");
                            }
                        });
                    }

                    // Winner
                    if (match.getWinner() != null) {
                        System.out.println("\n🏆 Winner: " + match.getWinner().getName());
                    }

                    // Betting metadata
                    if (match.getBettingMetadata() != null) {
                        System.out.println("\n📈 Betting:");
                        System.out.println("   Bookable:    " + match.getBettingMetadata().isBookable());
                        System.out.println("   Booked:      " + match.getBettingMetadata().isBooked());
                        System.out.println("   Live:        " + match.getBettingMetadata().isLiveAvailable());
                    }

                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        });

        System.out.println("✓ Connected! Monitoring fixtures...\n");
        Thread.currentThread().join();
    }
}
