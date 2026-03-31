# PandaScore Java SDK

Modern Java SDK for consuming live esports odds and match data from PandaScore's real-time feed.

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue)](https://adoptium.net/)
[![Build Tool](https://img.shields.io/badge/Build-Gradle-green)](https://gradle.org/)
[![GitHub Packages](https://img.shields.io/badge/GitHub%20Packages-1.0.0-blue)](https://github.com/PandaScore/pandascore-sdk-java/packages)

## 🎯 Overview

The PandaScore Java SDK provides a complete solution for integrating esports betting data into your applications. It combines real-time streaming via RabbitMQ with REST API endpoints for data recovery, featuring:

- **Real-time odds updates** via AMQPS feed
- **Automatic reconnection** with smart recovery
- **HTTP API** for on-demand queries
- **Production-ready** with extensive logging and error handling

## ✨ Features

### Core Functionality
- 🔄 **RabbitMQ Feed Integration** – Stream live markets, fixtures, and scoreboards
- 🔌 **Automatic Reconnection** – Detects disconnections and recovers missed data
- 📊 **Rich Data Models** – Complete type coverage for all message types
- 🌐 **HTTP Client** – Fetch matches and markets on-demand
- 📈 **Multiple Odds Formats** – Decimal, American, and Fractional

## 📋 Requirements

- **Java 17** or higher
- **Gradle** (wrapper included - `./gradlew`)
- Valid PandaScore API credentials

## 🚀 Quick Start

### 1. Add Dependency

The SDK is published to GitHub Packages. Add the repository and dependency to your project.

**Gradle (`build.gradle.kts`):**
```kotlin
repositories {
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

**Maven (`pom.xml`):**
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

Add credentials to `~/.m2/settings.xml` (Maven only):
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

### 2. Configure Credentials

Edit any example file or create your own:

```java
SDKOptions options = SDKOptions.builder()
    .apiToken("YOUR_API_TOKEN")
    .companyId(YOUR_COMPANY_ID)
    .email("your-email@example.com")
    .password("your-password")
    .queueBinding(
        SDKOptions.QueueBinding.builder()
            .queueName("my-queue")
            .routingKey("#")  // All messages
            .build()
    )
    .build();

SDKConfig.setOptions(options);
```

### 3. Connect and Receive Odds

```java
ObjectMapper mapper = new ObjectMapper()
    .registerModule(new JavaTimeModule());

EventHandler handler = new EventHandler(event -> {
    if (event.getCode() == ConnectionEvent.CODE_DISCONNECTION) {
        System.out.println("Disconnected!");
    } else {
        System.out.println("Reconnected with " +
            event.getRecoveryData().getMarkets().size() + " recovered markets");
    }
});

RabbitMQFeed feed = new RabbitMQFeed(handler);
feed.connect(message -> {
    JsonNode json = (JsonNode) message;

    if ("markets".equals(json.get("type").asText())) {
        MarketsMessage markets = mapper.treeToValue(json, MarketsMessage.class);

        markets.getMarkets().forEach(market -> {
            System.out.println("Market: " + market.getName());
            market.getSelections().forEach(sel -> {
                System.out.printf("  %s: %.2f%n",
                    sel.getName(),
                    sel.getOddsDecimalWithOverround()
                );
            });
        });
    }
});
```

### 4. Run Example

If you cloned the repository directly:

```bash
chmod +x gradlew
./gradlew run
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [QUICKSTART.md](QUICKSTART.md) | Complete setup guide with code examples |
| [API Documentation](https://pandaodds.readme.io/) | Official PandaScore API documentation |

## 🎮 Example

The SDK includes a comprehensive example showing all core functionality:

**BasicExample.java** - Production-ready template that:
- Monitors all message types (markets, fixtures, scoreboards)
- Handles disconnection/reconnection events
- Demonstrates proper JSON parsing and error handling
- Includes RabbitMQ feed connection setup
- Relies on INFO logs for message visibility

See [examples/README.md](src/main/java/com/pandascore/sdk/examples/README.md) for detailed usage.

## ⚙️ Configuration Options

### Required Fields

```java
SDKOptions options = SDKOptions.builder()
    .apiToken("...")        // REST API authentication token
    .companyId(12345)       // Your PandaScore account ID
    .email("...")           // Account email
    .password("...")        // Account password
    .queueBinding(...)      // At least one queue binding
```

### Optional Configuration

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `feedHost` | String | `trading-feed.pandascore.co` | RabbitMQ hostname |
| `apiBaseUrl` | String | `https://api.pandascore.co/betting/matches` | REST API base URL |
| `americanOdds` | boolean | `false` | Compute American odds (+100, -200) |
| `fractionalOdds` | boolean | `false` | Compute fractional odds (3/2, 8/13) |
| `recoverOnReconnect` | boolean | `true` | Auto-recover markets on reconnection (global default) |
| `alwaysLogPayload` | boolean | `false` | Log all payloads at INFO level |

### Queue Bindings

Control which messages you receive using routing keys. Each connection supports up to **10 queue bindings**.

```java
.queueBinding(
    SDKOptions.QueueBinding.builder()
        .queueName("my-queue")
        .routingKey("#")  // All messages
        .build()
)
```

**Routing Key Format:**

Messages use the format: `{version}.{videogame_slug}.{event_type}.{event_id}.{type}.{action}`

**Recommendation:**
- Use `#` to receive all messages and filter in your application code based on the `type` field
- This provides maximum flexibility and ensures you don't miss any updates

## 🔗 Multiple Connections

The SDK supports up to **10 concurrent AMQP connections**, each with its own queue bindings (up to **10 queues per connection**). This lets you split traffic across dedicated connections for better isolation and throughput.

### Creating Multiple Connections

Each `RabbitMQFeed` instance represents a separate AMQP connection. Pass per-connection queue bindings via the constructor:

```java
// 1. Configure global SDK settings (shared by all connections)
SDKConfig.setOptions(options);

// 2. Connection #1: markets only — with recovery enabled
List<SDKOptions.QueueBinding> marketsBindings = List.of(
    SDKOptions.QueueBinding.builder()
        .queueName("markets-queue")
        .routingKey("*.*.*.markets.#")
        .build()
);

EventHandler marketsHandler = new EventHandler(event -> { /* ... */ });
RabbitMQFeed marketsFeed = new RabbitMQFeed(marketsHandler, marketsBindings, true);
marketsFeed.connect(marketsMessage -> { /* process markets */ });

// 3. Connection #2: fixtures + scoreboards — no recovery
List<SDKOptions.QueueBinding> fixturesBindings = List.of(
    SDKOptions.QueueBinding.builder()
        .queueName("fixtures-queue")
        .routingKey("*.*.*.fixture.#")
        .build(),
    SDKOptions.QueueBinding.builder()
        .queueName("scoreboards-queue")
        .routingKey("*.*.*.scoreboard.#")
        .build()
);

EventHandler fixturesHandler = new EventHandler(event -> { /* ... */ });
RabbitMQFeed fixturesFeed = new RabbitMQFeed(fixturesHandler, fixturesBindings, false);
fixturesFeed.connect(fixturesMessage -> { /* process fixtures/scoreboards */ });

// Monitor active connections
System.out.println("Active: " + RabbitMQFeed.getActiveConnectionCount());
```

### Per-Connection Recovery (`recoverOnReconnect`)

When a connection drops and reconnects, the SDK can automatically call recovery APIs (`recoverMarkets` + `fetchMatchesRange`) to fetch missed data. With multiple connections, **you should enable recovery on only one connection** to avoid redundant API calls:

```java
// The 3-argument constructor: RabbitMQFeed(handler, queueBindings, recoverOnReconnect)

// Primary connection — handles recovery
RabbitMQFeed primary = new RabbitMQFeed(handler1, bindings1, true);

// Secondary connections — skip recovery (primary handles it)
RabbitMQFeed secondary = new RabbitMQFeed(handler2, bindings2, false);

// Uses global SDKOptions.recoverOnReconnect setting (default: true)
RabbitMQFeed defaultFeed = new RabbitMQFeed(handler3, bindings3, null);
```

| `recoverOnReconnect` value | Behavior |
|---|---|
| `true` | Always calls recovery APIs on reconnect |
| `false` | Never calls recovery APIs on reconnect |
| `null` (or omitted) | Falls back to the global `SDKOptions.recoverOnReconnect` setting |

> **Important**: If you run 10 connections all with `recoverOnReconnect=true` (the default), a network blip causes 10 identical recovery API calls. Set `recoverOnReconnect=false` on connections that don't need recovery data.

### Limits

| Resource | Limit | Enforcement |
|---|---|---|
| Concurrent connections | 10 | Hard limit — `IllegalStateException` if exceeded |
| Queues per connection | 10 | Validated at construction time |

### Cleanup

Always close feeds when done to release connections:

```java
// try-with-resources
try (RabbitMQFeed feed = new RabbitMQFeed(handler, bindings, false)) {
    feed.connect(sink);
    // ...
}

// Or explicit close
feed.close();

// Or shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    marketsFeed.close();
    fixturesFeed.close();
}));
```

See [MultiConnectionExample.java](src/main/java/com/pandascore/sdk/examples/MultiConnectionExample.java) for a complete working example.

## 🔌 API Reference

### Streaming API (RabbitMQ)

```java
RabbitMQFeed feed = new RabbitMQFeed(eventHandler);
feed.connect(message -> {
    // Process JsonNode message
});
```

**Message Types:**
- **Markets** - Odds and betting markets
- **Fixtures** - Match and tournament information
- **Scoreboards** - Live scores and game state

### HTTP API (REST)

```java
// Fetch single match
FixtureMatch match = MatchesClient.fetchMatch("123456");

// Fetch all markets for a match
List<MarketsMessageMarket> markets = MatchesClient.fetchMarkets("123456");

// Recover markets since timestamp
List<MarketsRecoveryMatch> recovery =
    MatchesClient.recoverMarkets("2026-01-20T10:00:00Z");

// Fetch matches in time range
List<FixtureMatch> matches =
    MatchesClient.fetchMatchesRange("2026-01-20T10:00:00Z", "2026-01-20T11:00:00Z");
```

## 📦 Data Models

### Complete Type Coverage

The SDK includes comprehensive data models for all message types:

**Markets** (`com.pandascore.sdk.model.feed.markets`)
- `MarketsMessage` - Top-level markets update
- `MarketsMessageMarket` - Individual market with 50+ fields
- `MarketsMessageSelection` - Selection with odds in all formats
- `MarketAction` - Action enum (created, odds_changed, settled, etc.)

**Fixtures** (`com.pandascore.sdk.model.feed.fixtures`)
- `FixtureMessage` - Match/tournament updates
- `FixtureMatch` - Complete match details (37 fields)
- `League`, `Tournament`, `Videogame` - Tournament hierarchy
- `Game`, `GameMap`, `GameWinner` - Individual game details
- `Player`, `FixtureTeam`, `FixtureOpponent` - Participant data
- `Live`, `StreamInfo`, `Streams` - Streaming information
- `MatchStatus`, `MatchType`, `GameStatus` - Status enums

**Scoreboards** (`com.pandascore.sdk.model.feed.scoreboard`)
- `ScoreboardEsoccer`, `ScoreboardEbasketball`, `ScoreboardEhockey`
- `ScoreboardCs`, `ScoreboardDota2`, `ScoreboardLol`, `ScoreboardValorant`
- Timer objects with pause state and period tracking

## 🔄 Automatic Recovery & Disconnection Handling

The SDK automatically handles disconnections with a clear flow to help you manage market states:

```java
EventHandler handler = new EventHandler(event -> {
    if (event.getCode() == ConnectionEvent.CODE_DISCONNECTION) {
        // ⚠️ DISCONNECTED (code 100) - Suspend/close your markets immediately
        logger.warn("Feed disconnected - suspending markets");
        suspendAllMarkets();
    } else if (event.getCode() == ConnectionEvent.CODE_RECONNECTION) {
        // ✅ RECONNECTED (code 101) - Recovery complete, safe to reopen markets
        logger.info("Feed reconnected - recovery complete");
        // Recovery data is available in the event
        ConnectionEvent.RecoveryData data = event.getRecoveryData();
        logger.info("Recovered {} markets, {} matches",
            data.getMarkets().size(), data.getMatches().size());
        reopenMarkets();
    }
});
```

### Disconnection/Reconnection Flow

**When disconnection is detected** (after 3 consecutive missed heartbeats, ~30 seconds):
1. ⚠️ **ConnectionEvent with code 100** → Your callback is notified **immediately**
2. 👉 **Action required**: Suspend/close all markets on your side
3. SDK begins automatic reconnection attempts with exponential backoff

**When a real heartbeat message arrives after reconnection**:
1. 🔄 SDK logs: "Heartbeat restored - starting recovery"
2. 📦 SDK begins buffering incoming messages (continues consuming from RabbitMQ)
3. 📊 SDK calls `recoverMarkets(downtime)` to fetch updated markets
4. 📋 SDK calls `fetchMatchesRange(downtime, uptime)` to fetch modified matches
5. 📤 SDK processes all buffered messages (in order)
6. ✅ SDK logs: "Recovery complete - reconnection successful"
7. ✅ **ConnectionEvent with code 101** → Your callback is notified, with recovery data attached
8. 👉 **Action required**: Reopen/resume markets - data is now synchronized

### Important Notes

- **Event codes**: Disconnection = `ConnectionEvent.CODE_DISCONNECTION` (100), Reconnection = `ConnectionEvent.CODE_RECONNECTION` (101)
- **Recovery data**: The reconnection event includes `RecoveryData` with recovered markets and matches from the two recovery API calls
- **Customer callback timing**: You receive disconnection immediately, but reconnection **only after recovery completes**
- **Message buffering**: During recovery, incoming messages are buffered internally to prevent race conditions
- **Heartbeat detection**: Heartbeat messages are identified by having an `at` field and no `type` field
- **Disabling recovery**: Set `recoverOnReconnect(false)` in `SDKOptions` (global) or per-connection via `new RabbitMQFeed(handler, bindings, false)` to disable automatic recovery
- **Multiple connections**: With multiple connections, enable recovery on **only one** connection to avoid redundant API calls. See [Multiple Connections](#-multiple-connections) for details

### Example Log Output

```
11:20:07 [INFO] Disconnection detected
         ↓ (customer callback fires - suspend markets)
11:20:50 [INFO] Heartbeat restored - starting recovery
11:20:58 [INFO] Recovery complete - reconnection successful
         ↓ (customer callback fires - reopen markets)
```

## 📝 Logging

The SDK uses SLF4J with Logback for comprehensive logging:

**Log Files:**
- `sdk-debug.log` - Debug level only
- `sdk-info.log` - Info level only
- `sdk-warn.log` - Warning and error level

**MDC Context Tags:**
- `session` - Session UUID
- `customerId` - Company ID
- `feed` - Feed identifier
- `messageType` - Message type
- `operation` - Current operation
- `routingKey` - AMQP routing key

**Configuration:**
```xml
<!-- Customize MDC pattern via system property -->
-Dsdk.mdc.pattern="[session=%X{session}] [customerId=%X{customerId}]"
```

All appenders use `AsyncAppender` to prevent blocking feed processing.

## 🏗️ Building

### Compile

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Generate Javadoc

```bash
./gradlew javadoc
# Output: build/docs/javadoc/index.html
```

### Create JAR

```bash
./gradlew jar
# Output: build/libs/sdk.jar
```

## 🐛 Troubleshooting

### Permission Denied: ./gradlew

```bash
chmod +x gradlew
```

Or use:
```bash
sh gradlew build
```

### No Messages Received

- Verify routing key is correct (`#` for all messages)
- Check credentials are valid
- Ensure there are active matches
- Enable debug logging: `.alwaysLogPayload(true)`

### Connection Issues

- Verify firewall allows AMQPS (port 5671)
- Check `feedHost` is reachable
- Review logs in `sdk-warn.log`

### Parsing Errors

- Ensure Jackson dependencies are included
- Verify you're using correct model class for message type
- Check raw JSON with `.alwaysLogPayload(true)`

## 🔗 Links

- **API Documentation**: https://pandaodds.readme.io/
- **Examples Guide**: [examples/README.md](src/main/java/com/pandascore/sdk/examples/README.md)
- **Quick Start**: [QUICKSTART.md](QUICKSTART.md)

## 📄 License

Copyright © 2026 PandaScore. All rights reserved.

---

**Ready to get started?** Check out [QUICKSTART.md](QUICKSTART.md) for a step-by-step guide!
