# PandaScore Java SDK Examples

Complete set of examples demonstrating different use cases for the Java SDK.

## 📋 Quick Reference

| Example | Use Case | Features |
|---------|----------|----------|
| `Example1_BasicOdds.java` | Getting started with odds | Simple odds display, American format |
| `Example2_FixtureUpdates.java` | Match status tracking | Fixture updates, match details |
| `Example3_SpecificMarkets.java` | Filter market types | Market filtering, multiple odds formats |
| `Example4_HTTPFetchMarkets.java` | One-time data fetch | HTTP API, snapshot queries |
| `Example5_AllMessageTypes.java` | Monitor everything | All message types, statistics |
| `SimpleGetOdds.java` | Minimal example | Copy-paste ready |
| `FeedConsole.java` | Production-ready template | Full featured with recovery |

---

## 🚀 Running Examples

### Option 1: Run with Gradle

```bash
# Make gradlew executable (one time)
chmod +x gradlew

# Run specific example
./gradlew run --args="com.pandascore.sdk.examples.Example1_BasicOdds"
```

### Option 2: Compile and Run with Java

```bash
# Build the project
./gradlew build

# Run example
java -cp build/libs/sdk.jar com.pandascore.sdk.examples.Example1_BasicOdds
```

### Option 3: From IDE

1. Open project in IntelliJ IDEA / Eclipse
2. Navigate to example file
3. Right-click and select "Run"

---

## 📚 Example Details

### Example 1: Basic Odds Display
**File**: `Example1_BasicOdds.java`

**Perfect for**: Getting started quickly

**What it does**:
- Connects to PandaScore feed
- Displays odds in decimal and American format
- Shows win probabilities
- Clean, formatted output

**Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⚡ ODDS UPDATE
   Match: 1313453
   Game: cs-go
   Action: odds_changed
   Time: 2026-01-20T10:45:13.111Z

   📊 Winner 2-Way [active]
      Team A                    2.50   +150  [40.0%]
      Team B                    1.62   -161  [60.0%]
```

**Routing key**: `pandascore.markets.#` (markets only)

---

### Example 2: Fixture Updates
**File**: `Example2_FixtureUpdates.java`

**Perfect for**: Tracking match status changes

**What it does**:
- Monitors fixture/match updates
- Shows match status changes (started, finished, settled)
- Displays full match details
- Shows league and tournament info

**Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎮 FIXTURE UPDATE: STARTED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Match:       Team A vs Team B
ID:          1313453
Status:      live
Game:        cs-go
Scheduled:   2026-01-20T10:00:00Z
Started:     2026-01-20T10:05:00Z

League:      ESL Pro League
Tournament:  Season 19
Tier:        s

👥 Opponents:
   • Team A (TA)
   • Team B (TB)
```

**Routing key**: `pandascore.fixtures.#` (fixtures only)

---

### Example 3: Specific Market Types
**File**: `Example3_SpecificMarkets.java`

**Perfect for**: Tracking only certain bet types

**What it does**:
- Filters for specific market templates
- Shows multiple odds formats (decimal, American, fractional)
- Displays market metadata (overround, status)
- Easily customizable filters

**Market filters** (modify in code):
```java
private static final List<String> MARKET_FILTERS = Arrays.asList(
    "winner-2-way",
    "winner-3-way",
    "correct-score"
);
```

**Output**:
```
======================================================================
📌 FILTERED MARKETS
   Match: 1313453
   Game:  cs-go
   Action: created
======================================================================

🎯 Winner 2-Way [winner-2-way]
   Status: active
   Overround: 104.0%
   Team A               │   2.50 │   +150 │     3/2 │ 40.0%
   Team B               │   1.62 │   -161 │    8/13 │ 60.0%
```

**Routing key**: `pandascore.markets.#`

---

### Example 4: HTTP API - Fetch Markets
**File**: `Example4_HTTPFetchMarkets.java`

**Perfect for**: One-time queries, building snapshots

**What it does**:
- Uses HTTP endpoints instead of streaming
- Fetches match details by ID
- Gets all markets for a specific match
- No persistent connection needed

**Methods used**:
- `MatchesClient.fetchMatch(id)` - Get match details
- `MatchesClient.fetchMarkets(matchId)` - Get all markets

**Output**:
```
📋 Fetching match details...

Match: Team A vs Team B
Status: live
Scheduled: 2026-01-20T10:00:00Z
League: ESL Pro League

Teams:
  • Team A
  • Team B

============================================================
📊 Fetching markets for match 1313453...

Found 156 markets

🎯 Winner 2-Way
   Template: winner-2-way
   Status: active
   Selections:
      Team A                     2.50  (+150)  [40.0%]
      Team B                     1.62  (-161)  [60.0%]
```

**No routing key needed** (HTTP only)

---

### Example 5: All Message Types
**File**: `Example5_AllMessageTypes.java`

**Perfect for**: Monitoring full feed activity

**What it does**:
- Processes all message types (markets, fixtures, scoreboards)
- Tracks message counts
- Prints statistics every 10 seconds
- Shows comprehensive feed overview

**Output**:
```
✓ Connected! Monitoring all message types...
  Stats will be printed every 10 seconds.

📊 MARKETS - Match #1313453
   Action: odds_changed
   Markets: 3
   Game: cs-go
   First: Winner 2-Way

🎮 FIXTURE - STARTED
   Match ID: 1313453
   Event: match #1313453
   Game: cs-go
   Name: Team A vs Team B
   Status: live

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📈 MESSAGE STATISTICS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Markets:        342
   Fixtures:        18
   Scoreboards:     45
   Other:            0
   TOTAL:          405
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Routing key**: `#` (all messages)

---

## 🔧 Configuration

All examples need credentials. Update these values:

```java
SDKOptions options = SDKOptions.builder()
    .apiToken("YOUR_TOKEN")           // ← Replace with your API token
    .companyId(12345)                 // ← Replace with your company ID
    .email("your-email@example.com")  // ← Replace with your email
    .password("your-password")        // ← Replace with your password
    .queueBinding(...)
    .build();
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `apiToken` | String | **Required** | REST API authentication |
| `companyId` | long | **Required** | Your company ID |
| `email` | String | **Required** | Account email |
| `password` | String | **Required** | Account password |
| `americanOdds` | boolean | `false` | Enable American odds format |
| `fractionalOdds` | boolean | `false` | Enable fractional odds format |
| `recoverOnReconnect` | boolean | `true` | Auto-recover on reconnection |

---

## 🎯 Routing Keys

Control which messages you receive:

| Routing Key | Receives |
|-------------|----------|
| `#` | All messages |
| `pandascore.markets.#` | All market updates |
| `pandascore.fixtures.#` | All fixture updates |
| `pandascore.markets.*.*.created` | Only market creation events |
| `pandascore.fixtures.*.*.started` | Only match start events |
| `pandascore.markets.match.*.odds_changed` | Only odds changes for matches |

---

## 📊 Message Types

### Markets Message
Odds and betting markets.

**Actions**: `created`, `odds_changed`, `suspended`, `deactivated`, `settled`, `rollback_settlement`

**Key fields**:
- `markets[]` - Array of markets
  - `template` - Market type (e.g., "winner-2-way")
  - `status` - "active", "suspended", "deactivated"
  - `selections[]` - Betting options
    - `oddsDecimal`, `oddsAmerican`, `oddsFractional`
    - `probability` - Win probability

### Fixture Message
Match and tournament information.

**Actions**: `created`, `booked`, `started`, `finished`, `settled`, `canceled`

**Key fields**:
- `match` - Match details
  - `name`, `status`, `scheduledAt`
  - `opponents[]` - Teams/players
  - `league`, `tournament` - Competition info
  - `winner` - Match result (if finished)

### Scoreboard Message
Live scores and game state.

**Types**: `esoccer`, `ebasketball`, `cs`, `dota2`, `lol`, `valorant`, `ehockey`

**Key fields**:
- `games[]` - Individual maps/games
  - Game-specific scoring (kills, rounds, goals, etc.)
  - Timer information
  - Current period/half/quarter

---

## 💡 Tips

### Starting Fresh
If this is your first time, start with **Example1_BasicOdds.java** - it's the simplest.

### Production Use
Use **FeedConsole.java** as a template - it includes:
- Error handling
- Reconnection logic
- Comprehensive logging
- MDC context

### Filtering Markets
Modify the filters in **Example3_SpecificMarkets.java** to track only the bet types you care about.

### HTTP vs Streaming
- **HTTP** (Example 4): One-time queries, building initial state
- **Streaming** (Examples 1-3, 5): Real-time updates, live monitoring

### Performance
- Use specific routing keys to reduce traffic
- Filter messages in your callback to process only what you need
- Enable `alwaysLogPayload(false)` in production to reduce log size

---

## 🐛 Troubleshooting

### No messages received
- Check routing key is correct (`#` for all messages)
- Verify credentials are valid
- Ensure there are active matches with markets

### Connection issues
- Verify firewall allows AMQPS (port 5671)
- Check `feedHost` is reachable
- Enable debug logging to see connection attempts

### Parsing errors
- Ensure Jackson dependencies are included
- Check you're using the correct model class for message type
- Enable payload logging to see raw JSON

---

## 📖 Next Steps

1. **Choose an example** that matches your use case
2. **Update credentials** in the example file
3. **Run the example** using one of the methods above
4. **Customize** the example to fit your needs

For more information:
- Check `QUICKSTART.md` for basic setup
- Read `IMPROVEMENTS_NEEDED.md` for full data model documentation
- Explore model classes in `src/main/java/com/pandascore/sdk/model/`

## 🆘 Support

Need help?
- GitHub Issues: https://github.com/jernej19/java-sdk/issues
- API Docs: https://developers.pandascore.co
