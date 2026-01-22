# PandaScore Java SDK Example

Production-ready example demonstrating real-time esports odds integration.

## 📋 Overview

**BasicExample.java** - Comprehensive example showing all SDK capabilities:
- Markets (odds updates)
- Fixtures (match updates)
- Scoreboards (live scores)
- Disconnection/reconnection handling

---

## 🚀 Running the Example

### Option 1: Run with Gradle

```bash
# Make gradlew executable (one time)
chmod +x gradlew

# Run BasicExample
./gradlew run --args="com.pandascore.sdk.examples.BasicExample"
```

### Option 2: Compile and Run with Java

```bash
# Build the project
./gradlew build

# Run example
java -cp build/libs/sdk.jar com.pandascore.sdk.examples.BasicExample
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
- ✅ Programmatic console output (key=value format)
- ✅ Error handling for parsing failures
- ✅ Real-time message display

**Sample Output**:
```
=== PandaScore SDK Basic Example ===

✓ Connected! Monitoring all message types...

MARKETS: matchId=1313453 action=odds_changed markets=12 game=cs-go
FIXTURE: matchId=1313453 eventType=match action=started game=cs-go name="Team A vs Team B" status=live
FIXTURE: matchId=1313453 eventType=game gameId=119445 action=started game=cs-go name="Team A vs Team B" status=live
SCOREBOARD: type=csgo id=1313453 games=3

[11:23:45] 🔔 DISCONNECTION
[11:24:12] 🔔 RECONNECTION
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

The example uses `#` (all messages) and filters by message type in code:

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

The example demonstrates proper disconnection/reconnection handling:

```java
EventHandler eventHandler = new EventHandler(event -> {
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    System.out.println("[" + time + "] 🔔 " + event.toUpperCase());
});
```

**Important**:
- **"disconnection"** fires immediately when connection is lost → Suspend markets
- **"reconnection"** fires AFTER automatic recovery completes → Reopen markets
- Recovery automatically fetches missed markets and match updates

---

## 🛠️ Customization Examples

### 1. Filter Specific Markets

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

### 2. Track Specific Game

```java
if ("markets".equals(type)) {
    MarketsMessage msg = mapper.treeToValue(json, MarketsMessage.class);

    // Only process CS:GO
    if ("cs-go".equals(msg.getVideogameSlug())) {
        System.out.println("CS:GO market update for match " + msg.getMatchId());
    }
}
```

### 3. Enable American Odds

Add to SDKOptions builder:
```java
.americanOdds(true)  // Enable American format (+150, -161)
```

Then access in your handler:
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

### 4. Monitor Match Status Changes

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

### 5. Store Data in Database

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
