# PandaScore Java SDK Examples

Production-ready examples demonstrating real-time esports odds integration.

## 📋 Available Examples

| Example | Description |
|---------|-------------|
| `BasicExample.java` | Monitors all message types with statistics tracking |
| `FeedConsole.java` | Production-ready template with market odds display |

---

## 🚀 Running Examples

### Option 1: Run with Gradle

```bash
# Make gradlew executable (one time)
chmod +x gradlew

# Run BasicExample
./gradlew run --args="com.pandascore.sdk.examples.BasicExample"

# Run FeedConsole
./gradlew run --args="com.pandascore.sdk.examples.FeedConsole"
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
2. Navigate to example file
3. Right-click and select "Run"

---

## 📚 Example Details

### BasicExample - All Message Types Monitor

**File**: `BasicExample.java`

**Perfect for**: Understanding all message types and SDK basics

**What it does**:
- Monitors markets, fixtures, and scoreboard messages
- Tracks message statistics (counts by type)
- Prints statistics every 10 seconds
- Handles disconnection/reconnection events

**Key Features**:
- ✅ All message types (markets, fixtures, scoreboards)
- ✅ Statistics tracking
- ✅ Event handling (disconnection/reconnection)
- ✅ Clean console output

**Output**:
```
=== PandaScore SDK Basic Example ===

✓ Connected! Monitoring all message types...
  Stats will be printed every 10 seconds.

📊 MARKETS - Match #1313453
   Action: odds_changed
   Markets: 12
   Game: cs-go
   First: Winner 2-Way

🎮 FIXTURE - STARTED
   Match ID: 1313453
   Event: match #12345
   Game: cs-go
   Name: Team A vs Team B
   Status: live

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📈 MESSAGE STATISTICS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Markets:          127
   Fixtures:          23
   Scoreboards:       45
   Other:              0
   TOTAL:            195
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### FeedConsole - Production Template

**File**: `FeedConsole.java`

**Perfect for**: Production deployment and market odds display

**What it does**:
- Connects to RabbitMQ feed
- Displays market odds in detailed format
- Shows decimal and American odds formats
- Includes disconnection/reconnection handling
- Production-ready error handling

**Key Features**:
- ✅ Detailed market odds display
- ✅ Multiple odds formats (decimal, American)
- ✅ Selection probabilities
- ✅ Event handling
- ✅ Production logging

**Output**:
```
Event markets #67890 - Market 'Winner 2-Way' (status=active, template=winner)
  -> Team Alpha                     | Decimal:   2.50 | American:    +150 | Prob:  40.0%
  -> Team Beta                      | Decimal:   1.62 | American:    -161 | Prob:  60.0%

Event markets #67890 - Market 'Total Maps' (status=active, template=totals)
  -> Over 2.5                       | Decimal:   1.85 | American:     -118 | Prob:  53.1%
  -> Under 2.5                      | Decimal:   2.05 | American:    +105 | Prob:  46.9%
```

---

## ⚙️ Configuration

Both examples require valid PandaScore credentials. Edit the example files to add your credentials:

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

### Routing Keys

Both examples use `#` (all messages). You can filter in your code:

```java
feed.connect(message -> {
    JsonNode json = (JsonNode) message;
    String type = json.get("type").asText();

    switch (type) {
        case "markets" -> handleMarkets(json);
        case "fixture" -> handleFixture(json);
        case "scoreboard" -> handleScoreboard(json);
    }
});
```

---

## 🔄 Event Handling

Both examples handle disconnection and reconnection:

```java
EventHandler eventHandler = new EventHandler(event -> {
    if ("disconnection".equals(event)) {
        System.out.println("⚠️  Disconnected - suspend markets");
    } else if ("reconnection".equals(event)) {
        System.out.println("✅ Reconnected - markets recovered");
    }
});
```

**Important**:
- "disconnection" fires immediately when connection is lost
- "reconnection" fires AFTER automatic recovery completes
- Recovery fetches missed markets and match updates automatically

---

## 🛠️ Customization Tips

### Filter Specific Markets

```java
if ("markets".equals(type)) {
    MarketsMessage msg = mapper.treeToValue(json, MarketsMessage.class);

    // Only process "winner" markets
    msg.getMarkets().stream()
        .filter(m -> "winner".equals(m.getTemplate()))
        .forEach(market -> {
            System.out.println("Winner market: " + market.getName());
        });
}
```

### Enable Different Odds Formats

```java
SDKOptions options = SDKOptions.builder()
    // ... other settings ...
    .americanOdds(true)      // Enable American format (+150, -161)
    .fractionalOdds(true)    // Enable fractional format (3/2, 8/11)
    .build();
```

Then access via:
```java
selection.getOddsDecimalWithOverround()    // 2.50
selection.getOddsAmericanWithOverround()   // +150
selection.getOddsFractionalWithOverround() // "3/2"
```

### Track Specific Games

```java
// Only process CS:GO markets
if ("markets".equals(type)) {
    MarketsMessage msg = mapper.treeToValue(json, MarketsMessage.class);

    if ("cs-go".equals(msg.getVideogameSlug())) {
        // Process CS:GO odds
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

- Check the [API Documentation](https://pandaodds.readme.io/)
- Review the [QUICKSTART.md](../../../../../QUICKSTART.md) guide
- Contact PandaScore support for credential issues
