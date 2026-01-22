# PandaScore Java SDK

Modern Java SDK for consuming live esports odds and match data from PandaScore's real-time feed.

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue)](https://adoptium.net/)
[![Build Tool](https://img.shields.io/badge/Build-Gradle-green)](https://gradle.org/)

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

### 1. Extract and Build

```bash
# Extract the provided SDK package
unzip pandascore-sdk-java.zip
cd pandascore-sdk-java

# Make gradlew executable and build
chmod +x gradlew
./gradlew build
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

EventHandler handler = new EventHandler(event ->
    System.out.println("Event: " + event)
);

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

```bash
# Run the example
./gradlew run --args="com.pandascore.sdk.examples.BasicExample"
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
| `recoverOnReconnect` | boolean | `true` | Auto-recover markets on reconnection |
| `alwaysLogPayload` | boolean | `false` | Log all payloads at INFO level |

### Queue Bindings

Control which messages you receive using routing keys:

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
    if ("disconnection".equals(event)) {
        // ⚠️ DISCONNECTED - Suspend/close your markets immediately
        logger.warn("Feed disconnected - suspending markets");
        suspendAllMarkets();
    } else if ("reconnection".equals(event)) {
        // ✅ RECONNECTED - Recovery complete, safe to reopen markets
        logger.info("Feed reconnected - recovery complete");
        reopenMarkets();
    }
});
```

### Disconnection/Reconnection Flow

**When disconnection is detected** (after 30 seconds without heartbeat):
1. ⚠️ **"disconnection" event fired** → Your callback is notified **immediately**
2. 👉 **Action required**: Suspend/close all markets on your side
3. SDK begins automatic reconnection attempts with exponential backoff

**When heartbeat is restored**:
1. 🔄 SDK logs: "Heartbeat restored - starting recovery"
2. 📦 SDK begins buffering incoming messages (continues consuming from RabbitMQ)
3. 📊 SDK calls `recoverMarkets(downtime)` to fetch updated markets
4. 📋 SDK calls `fetchMatchesRange(downtime, uptime)` to fetch modified matches
5. 📤 SDK processes all buffered messages (in order)
6. ✅ SDK logs: "Recovery complete - reconnection successful"
7. ✅ **"reconnection" event fired** → Your callback is notified
8. 👉 **Action required**: Reopen/resume markets - data is now synchronized

### Important Notes

- **Customer callback timing**: You receive "disconnection" immediately, but "reconnection" **only after recovery completes**
- **Message buffering**: During recovery, incoming messages are buffered internally to prevent race conditions
- This ensures you have fresh data before reopening markets, with proper message ordering
- Set `recoverOnReconnect(false)` to disable automatic recovery and handle it manually

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

## 🆘 Support

- **Issues**: https://github.com/jernej19/java-sdk/issues
- **Documentation**: See files listed above
- **Email**: support@pandascore.co

---

**Ready to get started?** Check out [QUICKSTART.md](QUICKSTART.md) for a step-by-step guide!
