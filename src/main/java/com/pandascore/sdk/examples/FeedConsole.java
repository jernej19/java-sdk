package com.pandascore.sdk.examples;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.http.MatchesClient;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;
import com.pandascore.sdk.model.feed.markets.MarketsMessageMarket;
import com.pandascore.sdk.model.feed.markets.MarketsMessageSelection;
import com.pandascore.sdk.rmq.RabbitMQFeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Simple console example that connects to the feed and prints market updates.
 */
public class FeedConsole {
    private static final Logger logger = LoggerFactory.getLogger(FeedConsole.class);

    public static void main(String[] args) throws Exception {
        // Start MDC session
        MDC.put("session", UUID.randomUUID().toString());

        // 1) Quick-start credentials & queue
        SDKOptions options = SDKOptions.builder()
                .apiToken("YOUR_TOKEN")
                .companyId(12345) // YOUR COMPANY ID
                .email("EMAIL")
                .password("PASSWORD")
                .queueBinding(
                        SDKOptions.QueueBinding.builder()
                                .queueName("QUEUE_NAME")
                                .routingKey("#")
                                .build()
                )
                .americanOdds(true)     // Enable American odds
                .alwaysLogPayload(false)
                .build();

        SDKConfig.setOptions(options);

        logger.info("Starting SDK for customer {}", options.getCompanyId());

        // 2) Disconnection/Reconnection handler
        final Instant[] downAt = {null};
        EventHandler eh = new EventHandler(evt -> {
            if ("disconnection".equals(evt)) {
                downAt[0] = Instant.now();
                logger.warn("Detected disconnection at {}", downAt[0]);
            } else if ("reconnection".equals(evt)) {
                Instant up = Instant.now();
                logger.info("Detected reconnection at {}", up);
                // Recovery logic...
            }
        });

        // 3) Prepare Jackson mapper
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new JavaTimeModule());

        // 4) Connect and consume
        RabbitMQFeed feed = new RabbitMQFeed(eh);
        feed.connect(obj -> {
            JsonNode json = (JsonNode) obj;

            // Only handle "markets" messages here
            if (!"markets".equals(json.get("type").asText())) {
                return;
            }

            // Envelope fields
            String eventType = json.get("event_type").asText();
            long eventId = json.get("event_id").asLong();

            try {
                // Deserialize into our DTO
                MarketsMessage msg = mapper.treeToValue(json, MarketsMessage.class);
                List<MarketsMessageMarket> markets = msg.getMarkets();

                // Print structured info
                for (MarketsMessageMarket m : markets) {
                    System.out.printf(
                            "Event %s #%d - Market '%s' (status=%s, template=%s)%n",
                            eventType, eventId,
                            m.getName(), m.getStatus(), m.getTemplate()
                    );
                    for (MarketsMessageSelection sel : m.getSelections()) {
                        System.out.printf(
                                "  -> %-30s | Decimal: %6.2f | American: %7s | Prob: %5.1f%%%n",
                                sel.getName(),
                                sel.getOddsDecimalWithOverround(),
                                formatAmerican(sel.getOddsAmericanWithOverround()),
                                sel.getProbabilityWithOverround() * 100
                        );
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to parse MarketsMessage", e);
            }
        });

        logger.info("Feed running … press Ctrl-C to quit.");
        new CountDownLatch(1).await();
    }

    /**
     * Format American odds with proper sign.
     */
    private static String formatAmerican(Double american) {
        if (american == null) return "N/A";
        return (american > 0 ? "+" : "") + String.format("%.0f", american);
    }
}
