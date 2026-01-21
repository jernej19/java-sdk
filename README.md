# PandaScore Java SDK

Modern Java SDK for consuming live esports odds and match data from PandaScore's real-time feed.

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue)](https://adoptium.net/)
[![Build Tool](https://img.shields.io/badge/Build-Gradle-green)](https://gradle.org/)

## 🎯 Overview

The PandaScore Java SDK provides a complete solution for integrating esports betting data into your applications. It combines real-time streaming via RabbitMQ with REST API endpoints for data recovery, featuring:

- **Real-time odds updates** via AMQPS feed
- **Automatic reconnection** with smart recovery
- **Comprehensive data models** for 7+ esports titles
- **HTTP API** for on-demand queries
- **Production-ready** with extensive logging and error handling

## ✨ Features

### Core Functionality
- 🔄 **RabbitMQ Feed Integration** – Stream live markets, fixtures, and scoreboards
- 🔌 **Automatic Reconnection** – Detects disconnections and recovers missed data
- 📊 **Rich Data Models** – Complete type coverage for all message types
- 🌐 **HTTP Client** – Fetch matches and markets on-demand
- 📈 **Multiple Odds Formats** – Decimal, American, and Fractional
- 🎮 **Multi-Sport Support** – CS:GO, Dota 2, LoL, Valorant, eSoccer, eBasketball, eHockey

### Recent Additions (v2.0)
- ✅ **18 new data type classes** including League, Tournament, Game, Player, and streaming types
- ✅ **28 new fields** in FixtureMatch for complete tournament hierarchy
- ✅ **2 new HTTP methods** (fetchMatch, fetchMarkets)
- ✅ **eHockey support** with full scoreboard tracking
- ✅ **Timer objects** for accurate live game timing
- ✅ **5 production-ready examples** covering all use cases

## 📋 Requirements

- **Java 17** or higher
- **Gradle** (wrapper included - `./gradlew`)
- Valid PandaScore API credentials

## 🚀 Quick Start

### 1. Clone and Build

```bash
git clone https://github.com/jernej19/java-sdk.git
cd java-sdk
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
    .americanOdds(true)      // Enable American odds
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
                System.out.printf("  %s: %.2f (%s)%n",
                    sel.getName(),
                    sel.getOddsDecimalWithOverround(),
                    formatAmerican(sel.getOddsAmericanWithOverround())
                );
            });
        });
    }
});
```

### 4. Run Examples

```bash
# Basic odds display
./gradlew run --args="com.pandascore.sdk.examples.Example1_BasicOdds"

# Match/fixture updates
./gradlew run --args="com.pandascore.sdk.examples.Example2_FixtureUpdates"

# Filter specific markets
./gradlew run --args="com.pandascore.sdk.examples.Example3_SpecificMarkets"

# HTTP API usage
./gradlew run --args="com.pandascore.sdk.examples.Example4_HTTPFetchMarkets"

# Monitor all message types
./gradlew run --args="com.pandascore.sdk.examples.Example5_AllMessageTypes"
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [QUICKSTART.md](QUICKSTART.md) | Complete setup guide with code examples |
| [examples/README.md](src/main/java/com/pandascore/sdk/examples/README.md) | Detailed guide to all 5+ examples |
| [API Documentation](https://pandaodds.readme.io/) | Official PandaScore API documentation |

## 🎮 Examples Overview

The SDK includes 5+ production-ready examples for different use cases:

| Example | Use Case | Key Features |
|---------|----------|--------------|
| **Example1_BasicOdds** | Getting started | Simple odds display, American format |
| **Example2_FixtureUpdates** | Match tracking | Status changes, teams, tournaments |
| **Example3_SpecificMarkets** | Market filtering | Filter by template, multiple formats |
| **Example4_HTTPFetchMarkets** | On-demand queries | REST API, no streaming required |
| **Example5_AllMessageTypes** | Full monitoring | All messages, statistics, comprehensive |
| **SimpleGetOdds** | Minimal example | Copy-paste ready starter |
| **FeedConsole** | Production template | Full-featured with recovery |

See [examples/README.md](src/main/java/com/pandascore/sdk/examples/README.md) for detailed documentation.

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
        .routingKey("#")  // Pattern
        .build()
)
```

**Common routing key patterns:**
- `#` - All messages (recommended - filter in your code as needed)

> **Note**: To filter for specific message types (markets, fixtures, scoreboards), use the `#` routing key and filter messages in your application code based on the `type` field.

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

## 🎯 Common Use Cases

### 1. Live Odds Display

```java
feed.connect(message -> {
    if (isMarkets(message)) {
        MarketsMessage markets = parse(message);
        displayOdds(markets);
    }
});
```

See: [Example1_BasicOdds.java](src/main/java/com/pandascore/sdk/examples/Example1_BasicOdds.java)

### 2. Match Status Tracking

```java
feed.connect(message -> {
    if (isFixture(message)) {
        FixtureMessage fixture = parse(message);
        if ("started".equals(fixture.getAction())) {
            handleMatchStart(fixture.getMatch());
        }
    }
});
```

See: [Example2_FixtureUpdates.java](src/main/java/com/pandascore/sdk/examples/Example2_FixtureUpdates.java)

### 3. Specific Market Types

```java
List<String> wantedMarkets = List.of("winner-2-way", "correct-score");

markets.getMarkets().stream()
    .filter(m -> wantedMarkets.contains(m.getTemplate()))
    .forEach(this::processMarket);
```

See: [Example3_SpecificMarkets.java](src/main/java/com/pandascore/sdk/examples/Example3_SpecificMarkets.java)

### 4. Initial Data Snapshot

```java
// Fetch current state before streaming
FixtureMatch match = MatchesClient.fetchMatch(matchId);
List<MarketsMessageMarket> markets = MatchesClient.fetchMarkets(matchId);

// Then start streaming for updates
feed.connect(this::handleUpdates);
```

See: [Example4_HTTPFetchMarkets.java](src/main/java/com/pandascore/sdk/examples/Example4_HTTPFetchMarkets.java)

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
2. 📊 SDK calls `recoverMarkets(downtime)` to fetch updated markets
3. 📋 SDK calls `fetchMatchesRange(downtime, uptime)` to fetch modified matches
4. ✅ SDK logs: "Recovery complete - reconnection successful"
5. ✅ **"reconnection" event fired** → Your callback is notified
6. 👉 **Action required**: Reopen/resume markets - data is now synchronized

### Important Notes

- **Customer callback timing**: You receive "disconnection" immediately, but "reconnection" **only after recovery completes**
- This ensures you have fresh data before reopening markets
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
