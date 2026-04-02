package com.example;

import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.ConnectionEvent;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.rmq.RabbitMQFeed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class App {
    public static void main(String[] args) throws Exception {

        // -------------------------------------------------------
        // 1. Configure SDK — replace with your real credentials
        // -------------------------------------------------------
        SDKOptions options = SDKOptions.builder()
            .apiToken("fredlebookmaker")
            .companyId(3)
            .email("fakebookmaker@pandascore.co")
            .password("fredlebookmaker")
            .queueBinding(
                SDKOptions.QueueBinding.builder()
                    .queueName("test-queue")
                    .routingKey("#")  // "#" = receive all messages
                    .build()
            )
            .build();

        SDKConfig.setOptions(options);

        // -------------------------------------------------------
        // 2. Set up event handler for disconnection/reconnection
        // -------------------------------------------------------
        EventHandler eventHandler = new EventHandler(event -> {
            if (event.getCode() == ConnectionEvent.CODE_DISCONNECTION) {
                System.out.println("[EVENT] Disconnected! Suspend your markets.");
            } else if (event.getCode() == ConnectionEvent.CODE_RECONNECTION) {
                System.out.println("[EVENT] Reconnected! Recovered "
                    + event.getRecoveryData().getMarkets().size() + " markets, "
                    + event.getRecoveryData().getMatches().size() + " matches.");
            }
        });

        // -------------------------------------------------------
        // 3. Create JSON mapper
        // -------------------------------------------------------
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

        // -------------------------------------------------------
        // 4. Connect to the feed and process messages
        // -------------------------------------------------------
        RabbitMQFeed feed = new RabbitMQFeed(eventHandler);

        feed.connect(message -> {
            JsonNode json = (JsonNode) message;
            String type = json.has("type") ? json.get("type").asText() : "heartbeat";

            switch (type) {
                case "markets":
                    System.out.println("[MARKETS] Match " + json.get("match_id")
                        + " — action: " + json.get("action"));
                    break;
                case "fixture":
                    System.out.println("[FIXTURE] Match " + json.get("match_id")
                        + " — action: " + json.get("action"));
                    break;
                case "scoreboard":
                    System.out.println("[SCOREBOARD] Match " + json.get("match_id"));
                    break;
                default:
                    // Heartbeat or unknown — SDK handles heartbeats internally
                    break;
            }
        });

        System.out.println("Connected! Receiving live updates... (Ctrl+C to stop)");
        Thread.currentThread().join();
    }
}
