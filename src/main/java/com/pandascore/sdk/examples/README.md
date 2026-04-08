# PandaScore Java SDK Example

Production-ready example demonstrating real-time esports odds integration.

## 📋 Overview

**BasicExample.java** - Single-connection example showing all SDK capabilities:
- Markets (odds updates)
- Fixtures (match updates)
- Scoreboards (live scores)
- Disconnection/reconnection handling

**MultiConnectionExample.java** - Multi-connection example demonstrating:
- Multiple concurrent AMQP connections with different queue bindings
- Per-connection recovery control (`recoverOnReconnect`)
- Proper cleanup via shutdown hooks

---

## 🚀 Running the Example

### Option 1: Run with Gradle

```bash
# BasicExample is the default main class
./gradlew run
```

### Option 2: Compile and Run with Java

```bash
# Build the project
./gradlew build

# Run example (use the fat jar or set classpath)
java -cp "build/libs/*:build/classes/java/main" com.pandascore.sdk.examples.BasicExample
```

### Option 3: From IDE

1. Open project in IntelliJ IDEA / Eclipse
2. Navigate to `BasicExample.java`
3. Right-click and select "Run"

---

## 📚 Example Details

### BasicExample - All Message Types Monitor

**File**: `BasicExample.java`

**Perfect for**: Understanding SDK basics and monitoring all activity

**What it does**:
- Monitors markets, fixtures, and scoreboard messages
- Handles disconnection/reconnection events
- Demonstrates proper message parsing
- Displays real-time updates for all message types

**Key Features**:
- ✅ All message types (markets, fixtures, scoreboards)
- ✅ Event handling (disconnection/reconnection)
- ✅ Message parsing to verify structure
- ✅ Error handling for parsing failures
- ✅ INFO logs for message visibility

**Sample Output**:
```
=== PandaScore SDK Basic Example ===

✓ Connected! Monitoring all message types...

13:45:54.094 INFO [...] Event: type=markets eventType=match eventId=1314736 action=odds_changed
13:45:54.285 INFO [...] Event: type=fixture eventType=match eventId=1329664 action=started
13:45:54.293 INFO [...] Event: type=fixture eventType=game eventId=119445 action=started
13:45:54.299 INFO [...] Event: type=scoreboard scoreboardType=csgo id=1313453

11:23:45 INFO [...] Disconnection detected
11:24:12 INFO [...] Heartbeat restored - starting recovery
11:24:13 INFO [...] Recovery complete - reconnection successful
```

---

## ⚙️ Configuration

Edit `BasicExample.java` to add your PandaScore credentials:

```java
SDKOptions options = SDKOptions.builder()
    .apiToken("YOUR_API_TOKEN")          // Get from PandaScore dashboard
    .companyId(YOUR_COMPANY_ID)          // Your company ID
    .email("your-email@example.com")     // Your account email
    .password("your-password")            // Your account password
    .queueBinding(
        SDKOptions.QueueBinding.builder()
            .queueName("your-queue-name") // Your RabbitMQ queue name
            .routingKey("#")              // Subscribe to all messages
            .build()
    )
    .build();
```

### Routing Key

The example uses `#` (all messages) and filters by message type in code.

**Typed listener (recommended)** — auto-deserializes messages for you:

```java
feed.connect(new FeedListener() {
    @Override public void onMarkets(MarketsMessage msg) { /* ... */ }
    @Override public void onFixture(FixtureMessage msg) { /* ... */ }
    @Override public void onScoreboard(JsonNode raw, String type) { /* ... */ }
});
```

**Raw JSON consumer** — full control over deserialization:

```java
feed.connect(message -> {
    JsonNode json = (JsonNode) message;
    String type = json.get("type").asText();
    switch (type) {
        case "markets" -> handleMarkets(json, mapper);
        case "fixture" -> handleFixture(json, mapper);
        case "scoreboard" -> handleScoreboard(json);
    }
});
```

---

## 🔄 Event Handling

The example demonstrates proper disconnection/reconnection handling using `ConnectionEvent`:

```java
EventHandler eventHandler = new EventHandler(event -> {
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    if (event.getCode() == ConnectionEvent.CODE_DISCONNECTION) {
        System.out.println("[" + time + "] DISCONNECTION (code " + event.getCode() + ")");
    } else {
        ConnectionEvent.RecoveryData data = event.getRecoveryData();
        System.out.println("[" + time + "] RECONNECTION (code " + event.getCode() + ")");
        System.out.println("  Recovered " + data.getMarkets().size()
            + " markets, " + data.getMatches().size() + " matches");
        if (!data.isComplete()) {
            System.out.println("  WARNING: recovery was partial — some data may be missing");
        }
    }
});
```

**Important**:
- **Code 100 (disconnection)** fires immediately when 3 heartbeats are missed (~30s) → Suspend markets
- **Code 101 (reconnection)** fires AFTER automatic recovery completes → Reopen markets
- Recovery data (markets + matches) is included in the reconnection event
- **`RecoveryData.isComplete()`** returns `true` if both recovery API calls succeeded, `false` if recovery was partial (e.g. one API call timed out). Always check this before trusting the data
- Recovery automatically fetches missed markets and match updates

---

## 🔗 MultiConnectionExample - Multiple Feeds

**File**: `MultiConnectionExample.java`

**Perfect for**: Splitting traffic across dedicated connections

**What it does**:
- Creates two AMQP connections with different queue bindings
- Connection 1: markets only, with recovery enabled
- Connection 2: fixtures + scoreboards, recovery disabled
- Demonstrates per-connection `recoverOnReconnect` flag

**Key Points**:
- Only **one connection** should have `recoverOnReconnect=true` to avoid redundant recovery API calls
- Each connection has its own `EventHandler` for independent disconnection/reconnection tracking
- Max 10 connections, max 10 queues per connection

```java
// Connection 1: markets — handles recovery
RabbitMQFeed marketsFeed = new RabbitMQFeed(marketsHandler, marketsBindings, true);

// Connection 2: fixtures — no recovery
RabbitMQFeed fixturesFeed = new RabbitMQFeed(fixturesHandler, fixtureBindings, false);
```

---

## 🛠️ Customization Examples

### 1. Using FeedListener with Typed Callbacks

The `FeedListener` interface provides type-safe callbacks — no manual JSON parsing needed:

```java
feed.connect(new FeedListener() {
    @Override
    public void onMarkets(MarketsMessage markets) {
        markets.getMarkets().forEach(market -> {
            System.out.println("Market: " + market.getName());
            market.getSelections().forEach(sel -> {
                System.out.printf("  %s: %.2f%n",
                    sel.getName(), sel.getOddsDecimalWithOverround());
            });
        });
    }

    @Override
    public void onFixture(FixtureMessage fixture) {
        if (fixture.getMatch() != null) {
            System.out.println("Match: " + fixture.getMatch().getName()
                + " — " + fixture.getMatch().getStatus());
        }
    }

    @Override
    public void onScoreboard(JsonNode raw, String scoreboardType) {
        // Deserialize to the specific scoreboard model based on type
        switch (scoreboardType) {
            case "cs"   -> { /* mapper.treeToValue(raw, ScoreboardCs.class) */ }
            case "lol"  -> { /* mapper.treeToValue(raw, ScoreboardLol.class) */ }
            case "dota2" -> { /* mapper.treeToValue(raw, ScoreboardDota2.class) */ }
            // valorant, esoccer, ebasketball, ehockey, etennis
        }
    }
});
```

### 2. Filter Specific Markets

```java
private static void handleMarkets(JsonNode json, ObjectMapper mapper) throws Exception {
    MarketsMessage msg = mapper.treeToValue(json, MarketsMessage.class);

    // Only process "winner" markets
    msg.getMarkets().stream()
        .filter(m -> "winner".equals(m.getTemplate()))
        .forEach(market -> {
            System.out.println("Winner market: " + market.getName());
            market.getSelections().forEach(sel -> {
                System.out.printf("  %s: %.2f (%.1f%%)%n",
                    sel.getName(),
                    sel.getOddsDecimalWithOverround(),
                    sel.getProbabilityWithOverround() * 100
                );
            });
        });
}
```

### 3. Track Specific Game

```java
if ("markets".equals(type)) {
    MarketsMessage msg = mapper.treeToValue(json, MarketsMessage.class);

    // Only process CS:GO
    if ("cs-go".equals(msg.getVideogameSlug())) {
        System.out.println("CS:GO market update for match " + msg.getMatchId());
    }
}
```

### 4. Access American Odds

American odds are computed by default (false). To enable, add to SDKOptions builder:
```java
.americanOdds(true)  // Enable American format computation (+150, -161)
```

Access odds in your handler:
```java
market.getSelections().forEach(sel -> {
    System.out.printf("%s: Decimal %.2f | American %s%n",
        sel.getName(),
        sel.getOddsDecimalWithOverround(),
        formatAmerican(sel.getOddsAmericanWithOverround())
    );
});

// Helper method
private static String formatAmerican(Double american) {
    if (american == null) return "N/A";
    return (american > 0 ? "+" : "") + String.format("%.0f", american);
}
```

### 5. Monitor Match Status Changes

```java
private static void handleFixture(JsonNode json, ObjectMapper mapper) throws Exception {
    FixtureMessage msg = mapper.treeToValue(json, FixtureMessage.class);

    // Alert on status changes
    if (msg.getMatch() != null) {
        String status = msg.getMatch().getStatus();

        switch (status) {
            case "live" -> System.out.println("🔴 MATCH STARTED: " + msg.getMatch().getName());
            case "finished" -> System.out.println("✅ MATCH FINISHED: " + msg.getMatch().getName());
            case "canceled" -> System.out.println("❌ MATCH CANCELED: " + msg.getMatch().getName());
        }
    }
}
```

### 6. Store Data in Database

```java
private static void handleMarkets(JsonNode json, ObjectMapper mapper) throws Exception {
    MarketsMessage msg = mapper.treeToValue(json, MarketsMessage.class);

    // Save to your database
    for (MarketsMessageMarket market : msg.getMarkets()) {
        MarketEntity entity = new MarketEntity();
        entity.setMatchId(msg.getMatchId());
        entity.setMarketId(market.getId());
        entity.setName(market.getName());
        entity.setStatus(market.getStatus());
        // ... map other fields

        marketRepository.save(entity);
    }
}
```

---

## 📖 Additional Resources

- [Main README](../../../../../README.md) - SDK overview and features
- [QUICKSTART.md](../../../../../QUICKSTART.md) - Detailed setup guide
- [API Documentation](https://pandaodds.readme.io/) - REST API reference

---

## 💡 Need Help?

- **Documentation**: [https://pandaodds.readme.io/](https://pandaodds.readme.io/)
- **Setup Guide**: See [QUICKSTART.md](../../../../../QUICKSTART.md)
- **Credentials**: Contact PandaScore support for API access
