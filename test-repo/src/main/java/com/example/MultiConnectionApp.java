package com.example;

import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.ConnectionEvent;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.rmq.RabbitMQFeed;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Multi-connection example for the test repository.
 *
 * Demonstrates two separate AMQP connections:
 *   - Connection 1: markets feed with recovery enabled
 *   - Connection 2: fixtures + scoreboards feed with recovery disabled
 *
 * Only one connection should enable recovery to avoid redundant API calls.
 */
public class MultiConnectionApp {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Multi-Connection Test App ===\n");

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
                    .queueName("default-queue")
                    .routingKey("#")
                    .build()
            )
            .build();

        SDKConfig.setOptions(options);

        // -------------------------------------------------------
        // 2. Connection 1 — markets only, recovery ENABLED
        //    This connection handles recovery for missed data.
        // -------------------------------------------------------
        List<SDKOptions.QueueBinding> marketsBindings = List.of(
            SDKOptions.QueueBinding.builder()
                .queueName("markets-queue")
                .routingKey("*.*.*.markets.#")
                .build()
        );

        EventHandler marketsHandler = new EventHandler(event -> {
            logEvent("markets", event);
        });

        // Third argument = true → recovery enabled on this connection
        RabbitMQFeed marketsFeed = new RabbitMQFeed(marketsHandler, marketsBindings, true);
        marketsFeed.connect(message -> {
            JsonNode json = (JsonNode) message;
            System.out.printf("[MARKETS] match_id=%s action=%s%n",
                json.get("match_id"), json.get("action"));
        });

        // -------------------------------------------------------
        // 3. Connection 2 — fixtures + scoreboards, recovery DISABLED
        //    Recovery is handled by connection 1 so we skip it here.
        // -------------------------------------------------------
        List<SDKOptions.QueueBinding> fixtureBindings = List.of(
            SDKOptions.QueueBinding.builder()
                .queueName("fixtures-queue")
                .routingKey("*.*.*.fixture.#")
                .build(),
            SDKOptions.QueueBinding.builder()
                .queueName("scoreboards-queue")
                .routingKey("*.*.*.scoreboard.#")
                .build()
        );

        EventHandler fixturesHandler = new EventHandler(event -> {
            logEvent("fixtures", event);
        });

        // Third argument = false → recovery disabled on this connection
        RabbitMQFeed fixturesFeed = new RabbitMQFeed(fixturesHandler, fixtureBindings, false);
        fixturesFeed.connect(message -> {
            JsonNode json = (JsonNode) message;
            String type = json.has("type") ? json.get("type").asText() : "unknown";
            System.out.printf("[%s] match_id=%s%n",
                type.toUpperCase(), json.get("match_id"));
        });

        // -------------------------------------------------------
        // 4. Status & shutdown
        // -------------------------------------------------------
        System.out.println("Active connections: " + RabbitMQFeed.getActiveConnectionCount());
        System.out.println("Listening on 2 connections... (Ctrl+C to stop)\n");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            marketsFeed.close();
            fixturesFeed.close();
        }));

        Thread.currentThread().join();
    }

    private static void logEvent(String feedName, ConnectionEvent event) {
        String time = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (event.getCode() == ConnectionEvent.CODE_DISCONNECTION) {
            System.out.printf("[%s] [%s] DISCONNECTED — suspend markets%n", time, feedName);
        } else if (event.getCode() == ConnectionEvent.CODE_RECONNECTION) {
            System.out.printf("[%s] [%s] RECONNECTED — recovered %d markets, %d matches%n",
                time, feedName,
                event.getRecoveryData().getMarkets().size(),
                event.getRecoveryData().getMatches().size());
        }
    }
}
