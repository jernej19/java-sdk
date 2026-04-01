# PandaScore Java SDK

Java SDK for consuming live esports odds and match data from PandaScore's real-time feed.

- **Java 17+** required
- Published to **GitHub Packages** as a Maven dependency

## Installation

The SDK is published to GitHub Packages. You need a GitHub token with `read:packages` scope.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/PandaScore/pandascore-sdk-java")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("co.pandascore:pandascore-sdk-java:1.0.0")
}
```

### Maven

Add the repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/PandaScore/pandascore-sdk-java</url>
    </repository>
</repositories>

<dependency>
    <groupId>co.pandascore</groupId>
    <artifactId>pandascore-sdk-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

Add credentials to `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>${env.GITHUB_ACTOR}</username>
            <password>${env.GITHUB_TOKEN}</password>
        </server>
    </servers>
</settings>
```

## Connecting to the Feed

### 1. Configure the SDK

Call `SDKConfig.setOptions()` once at startup before creating any connections:

```java
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;

SDKOptions options = SDKOptions.builder()
    .apiToken("YOUR_API_TOKEN")
    .companyId(12345)
    .email("your-email@example.com")
    .password("your-password")
    .queueBinding(
        SDKOptions.QueueBinding.builder()
            .queueName("my-queue")
            .routingKey("#")   // "#" subscribes to all messages
            .build()
    )
    .build();

SDKConfig.setOptions(options);
```

### 2. Create a Connection

Each `RabbitMQFeed` instance opens one AMQP connection. You need an `EventHandler` to receive disconnection/reconnection notifications:

```java
import com.pandascore.sdk.events.ConnectionEvent;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.rmq.RabbitMQFeed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

EventHandler handler = new EventHandler(event -> {
    if (event.getCode() == ConnectionEvent.CODE_DISCONNECTION) {
        System.out.println("Disconnected - suspend your markets");
    } else if (event.getCode() == ConnectionEvent.CODE_RECONNECTION) {
        System.out.println("Reconnected - recovered "
            + event.getRecoveryData().getMarkets().size() + " markets");
    }
});

RabbitMQFeed feed = new RabbitMQFeed(handler);
feed.connect(message -> {
    JsonNode json = (JsonNode) message;
    String type = json.get("type").asText();

    switch (type) {
        case "markets" -> {
            MarketsMessage markets = mapper.treeToValue(json, MarketsMessage.class);
            // process odds updates
        }
        case "fixture" -> {
            FixtureMessage fixture = mapper.treeToValue(json, FixtureMessage.class);
            // process match updates
        }
        case "scoreboard" -> {
            // process live scores (game-specific: ScoreboardCs, ScoreboardLol, etc.)
        }
    }
});
```

### 3. Closing Connections

Always close feeds when your application shuts down:

```java
// Option A: try-with-resources
try (RabbitMQFeed feed = new RabbitMQFeed(handler)) {
    feed.connect(sink);
    Thread.currentThread().join();
}

// Option B: shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    feed.close();
}));
```

## Multiple Connections

The SDK supports up to **10 concurrent AMQP connections**, each with its own queue bindings (up to 10 queues per connection). This is how you run separate services (e.g., a logger and a market maker) from the same application or across separate deployments.

**Each connection needs its own `EventHandler` and `RabbitMQFeed`.**

### Example: Two Connections (Markets + Fixtures)

```java
// Global config (shared credentials, called once)
SDKConfig.setOptions(options);

// --- Connection 1: Markets (with recovery) ---
List<SDKOptions.QueueBinding> marketsBindings = List.of(
    SDKOptions.QueueBinding.builder()
        .queueName("markets-queue")
        .routingKey("#")
        .build()
);
EventHandler marketsHandler = new EventHandler(event -> { /* ... */ });
RabbitMQFeed marketsFeed = new RabbitMQFeed(marketsHandler, marketsBindings, true);
marketsFeed.connect(msg -> { /* process markets */ });

// --- Connection 2: Fixtures (no recovery) ---
List<SDKOptions.QueueBinding> fixturesBindings = List.of(
    SDKOptions.QueueBinding.builder()
        .queueName("fixtures-queue")
        .routingKey("#")
        .build()
);
EventHandler fixturesHandler = new EventHandler(event -> { /* ... */ });
RabbitMQFeed fixturesFeed = new RabbitMQFeed(fixturesHandler, fixturesBindings, false);
fixturesFeed.connect(msg -> { /* process fixtures */ });

// Check how many connections are active
System.out.println("Active connections: " + RabbitMQFeed.getActiveConnectionCount());
```

### Constructor Variants

```java
// Uses global queue bindings and recovery setting from SDKOptions
new RabbitMQFeed(handler);

// Per-connection queue bindings, global recovery setting
new RabbitMQFeed(handler, queueBindings);

// Per-connection queue bindings AND per-connection recovery setting
new RabbitMQFeed(handler, queueBindings, true);   // recovery enabled
new RabbitMQFeed(handler, queueBindings, false);  // recovery disabled
new RabbitMQFeed(handler, queueBindings, null);   // use global default
```

### Running from Separate Services / Docker Containers

If your logger and market maker run in **separate JVMs** (e.g., different Docker containers), each service creates its own `SDKConfig` and `RabbitMQFeed` independently. They share the same PandaScore credentials but must use **different queue names** to avoid consuming each other's messages:

```java
// --- Service A: Market Maker ---
SDKConfig.setOptions(SDKOptions.builder()
    .apiToken("YOUR_TOKEN").companyId(12345)
    .email("you@example.com").password("pass")
    .queueBinding(SDKOptions.QueueBinding.builder()
        .queueName("market-maker-queue")   // unique queue name
        .routingKey("#").build())
    .build());

EventHandler handler = new EventHandler(event -> { /* ... */ });
RabbitMQFeed feed = new RabbitMQFeed(handler);
feed.connect(msg -> { /* market maker logic */ });
```

```java
// --- Service B: Logger (separate JVM / Docker) ---
SDKConfig.setOptions(SDKOptions.builder()
    .apiToken("YOUR_TOKEN").companyId(12345)
    .email("you@example.com").password("pass")
    .queueBinding(SDKOptions.QueueBinding.builder()
        .queueName("logger-queue")         // different queue name
        .routingKey("#").build())
    .build());

EventHandler handler = new EventHandler(event -> { /* ... */ });
RabbitMQFeed feed = new RabbitMQFeed(handler);
feed.connect(msg -> { /* logging logic */ });
```

**Important**: If two services use the **same queue name**, RabbitMQ delivers each message to only one consumer (round-robin). Use distinct queue names so both services receive all messages independently.

## Recovery on Reconnect

When a connection drops and reconnects, the SDK can automatically call recovery APIs to fetch data you missed during the downtime. This is controlled by the `recoverOnReconnect` flag.

### How Recovery Works

1. Connection drops (detected after ~30s of missed heartbeats)
2. SDK emits `ConnectionEvent(code=100)` -- **you should suspend/close your markets**
3. SDK reconnects automatically with exponential backoff
4. First heartbeat arrives on the new connection
5. SDK buffers incoming messages and calls recovery APIs:
   - `recoverMarkets(downtime)` -- fetches updated market states
   - `fetchMatchesRange(downtime, uptime)` -- fetches modified matches
6. SDK replays buffered messages in order
7. SDK emits `ConnectionEvent(code=101)` with `RecoveryData` -- **you can reopen markets**

### Controlling Recovery per Connection

With multiple connections, **enable recovery on only one connection** to avoid redundant API calls:

```java
// Primary connection -- handles recovery for the whole system
RabbitMQFeed primary = new RabbitMQFeed(handler1, bindings1, true);

// Secondary connections -- skip recovery (primary already handles it)
RabbitMQFeed secondary = new RabbitMQFeed(handler2, bindings2, false);
RabbitMQFeed tertiary  = new RabbitMQFeed(handler3, bindings3, false);
```

| `recoverOnReconnect` value | Behavior |
|---|---|
| `true` | Calls recovery APIs on reconnect |
| `false` | Skips recovery APIs on reconnect |
| `null` (or omitted) | Falls back to global `SDKOptions.recoverOnReconnect` (default: `true`) |

### Disabling Recovery Globally

If you handle recovery yourself, disable it globally:

```java
SDKOptions options = SDKOptions.builder()
    // ... credentials ...
    .recoverOnReconnect(false)
    .build();
```

## Configuration Reference

### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `apiToken` | String | REST API authentication token |
| `companyId` | long | Your PandaScore account ID (must be positive) |
| `email` | String | Account email (AMQP login) |
| `password` | String | Account password (AMQP login) |
| `queueBindings` | List | At least one queue binding (max 10) |

### Optional Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `feedHost` | String | `trading-feed.pandascore.co` | AMQP broker hostname |
| `apiBaseUrl` | String | `https://api.pandascore.co/betting/matches` | REST API base URL |
| `recoverOnReconnect` | boolean | `true` | Auto-recover on reconnect (global default) |
| `prefetchCount` | int | `1` | Max unacked messages per consumer |
| `americanOdds` | boolean | `false` | Include American odds fields |
| `fractionalOdds` | boolean | `false` | Include fractional odds fields |
| `alwaysLogPayload` | boolean | `false` | Log payloads at INFO (otherwise DEBUG) |

### Limits

| Resource | Limit |
|----------|-------|
| Concurrent AMQP connections | 10 (throws `IllegalStateException` if exceeded) |
| Queue bindings per connection | 10 |

## HTTP API

The SDK also provides static HTTP methods for on-demand queries and recovery:

```java
import com.pandascore.sdk.http.MatchesClient;

// Fetch a single match
FixtureMatch match = MatchesClient.fetchMatch("123456");

// Fetch all markets for a match
List<MarketsMessageMarket> markets = MatchesClient.fetchMarkets("123456");

// Recover markets modified since a timestamp
List<MarketsRecoveryMatch> recovered =
    MatchesClient.recoverMarkets("2026-01-20T10:00:00Z");

// Fetch matches modified in a time range
List<FixtureMatch> matches =
    MatchesClient.fetchMatchesRange("2026-01-20T10:00:00Z", "2026-01-20T11:00:00Z");
```

These methods require `SDKConfig.setOptions()` to have been called first.

## Message Types

The feed delivers three types of messages, identified by the `type` field in the JSON:

| Type | Model Class | Description |
|------|-------------|-------------|
| `markets` | `MarketsMessage` | Odds and betting market updates |
| `fixture` | `FixtureMessage` | Match and tournament information |
| `scoreboard` | Game-specific (`ScoreboardCs`, `ScoreboardLol`, etc.) | Live scores and game state |

### Routing Keys

Messages use the format: `{version}.{videogame_slug}.{event_type}.{event_id}.{type}.{action}`

We recommend using `#` (all messages) and filtering by the `type` field in your application code. This ensures you don't miss any updates.

## Logging

The SDK uses SLF4J + Logback with async file appenders:

| Log File | Level |
|----------|-------|
| `logs/sdk-debug.log` | DEBUG |
| `logs/sdk-info.log` | INFO |
| `logs/sdk-warn.log` | WARN+ |

MDC context tags (`session`, `customerId`, `feed`, `messageType`, `operation`, `routingKey`) are included in every log entry.

## Troubleshooting

**No messages received**: Verify credentials, check that your routing key is correct (`#` for all messages), and confirm there are active matches. Enable debug logging with `.alwaysLogPayload(true)`.

**Connection refused**: Ensure your firewall allows AMQPS on port 5671. Verify `feedHost` is reachable. Check `logs/sdk-warn.log` for details.

**Second service can't connect**: If you run two services with the **same queue name**, they share messages via round-robin rather than both receiving all messages. Use a distinct queue name per service. See [Running from Separate Services](#running-from-separate-services--docker-containers).

**IllegalStateException: Cannot create more than 10 connections**: You have too many `RabbitMQFeed` instances open. Call `feed.close()` on feeds you no longer need. Monitor with `RabbitMQFeed.getActiveConnectionCount()`.

## Building from Source

```bash
./gradlew build    # compile + run tests
./gradlew test     # tests only
./gradlew run      # run BasicExample
./gradlew javadoc  # generate API docs
```

## Links

- [QUICKSTART.md](QUICKSTART.md) -- Step-by-step setup guide
- [API Documentation](https://pandaodds.readme.io/) -- Official PandaScore API docs
- [BasicExample.java](src/main/java/com/pandascore/sdk/examples/BasicExample.java) -- Single connection example
- [MultiConnectionExample.java](src/main/java/com/pandascore/sdk/examples/MultiConnectionExample.java) -- Multi-connection example

---

Copyright 2026 PandaScore. All rights reserved.
