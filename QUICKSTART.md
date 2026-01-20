# Java SDK Quick Start Guide

## Simple Example: Connect and Get Odds

### 1. Add Dependency

Add to your `build.gradle`:
```gradle
dependencies {
    implementation files('path/to/java-sdk.jar')
    // Or if published to Maven:
    // implementation 'com.pandascore:sdk:1.0.0'
}
```

Or `pom.xml`:
```xml
<dependency>
    <groupId>com.pandascore</groupId>
    <artifactId>sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Basic Usage Example

```java
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.rmq.RabbitMQFeed;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class SimpleOddsExample {
    public static void main(String[] args) throws Exception {

        // 1. Configure SDK with your credentials
        SDKOptions options = SDKOptions.builder()
            .apiToken("YOUR_API_TOKEN")
            .companyId(YOUR_COMPANY_ID)
            .email("your-email@example.com")
            .password("your-password")
            .queueBinding(
                SDKOptions.QueueBinding.builder()
                    .queueName("my-queue")
                    .routingKey("#")  // Subscribe to all messages
                    .build()
            )
            .americanOdds(true)      // Optional: enable American odds
            .fractionalOdds(false)   // Optional: enable fractional odds
            .build();

        SDKConfig.setOptions(options);

        // 2. Create event handler (for disconnection/reconnection)
        EventHandler eventHandler = new EventHandler(event -> {
            System.out.println("Event: " + event);
        });

        // 3. Create ObjectMapper for JSON parsing
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

        // 4. Connect to feed and process messages
        RabbitMQFeed feed = new RabbitMQFeed(eventHandler);

        feed.connect(message -> {
            JsonNode json = (JsonNode) message;
            String type = json.get("type").asText();

            // Process markets (odds) messages
            if ("markets".equals(type)) {
                try {
                    MarketsMessage markets = mapper.treeToValue(json, MarketsMessage.class);

                    System.out.println("=== MARKETS UPDATE ===");
                    System.out.println("Match ID: " + markets.getMatchId());
                    System.out.println("Action: " + markets.getAction());

                    markets.getMarkets().forEach(market -> {
                        System.out.println("\nMarket: " + market.getName());
                        System.out.println("  Status: " + market.getStatus());

                        market.getSelections().forEach(selection -> {
                            System.out.printf("    %s: %.2f%n",
                                selection.getName(),
                                selection.getOddsDecimalWithOverround()
                            );
                        });
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.println("Connected! Receiving odds updates...");
        Thread.currentThread().join(); // Keep running
    }
}
```

### 3. Fetch Specific Match Markets (HTTP API)

If you want to fetch markets for a specific match instead of streaming:

```java
import com.pandascore.sdk.http.MatchesClient;
import com.pandascore.sdk.model.feed.markets.MarketsMessageMarket;
import java.util.List;

public class FetchMarkets {
    public static void main(String[] args) throws Exception {

        // Configure SDK first (same as above)
        SDKOptions options = SDKOptions.builder()
            .apiToken("YOUR_API_TOKEN")
            .companyId(YOUR_COMPANY_ID)
            .email("your-email@example.com")
            .password("your-password")
            .queueBinding(/* ... */)
            .build();

        SDKConfig.setOptions(options);

        // Fetch markets for a specific match
        String matchId = "123456";
        List<MarketsMessageMarket> markets = MatchesClient.fetchMarkets(matchId);

        System.out.println("Found " + markets.size() + " markets");

        markets.forEach(market -> {
            System.out.println("\n" + market.getName());
            market.getSelections().forEach(selection -> {
                System.out.printf("  %s: %.2f (probability: %.2f%%)%n",
                    selection.getName(),
                    selection.getOddsDecimalWithOverround(),
                    selection.getProbabilityWithOverround() * 100
                );
            });
        });
    }
}
```

### 4. Fetch Match Details

```java
import com.pandascore.sdk.http.MatchesClient;
import com.pandascore.sdk.model.feed.fixtures.FixtureMatch;

public class FetchMatch {
    public static void main(String[] args) throws Exception {

        // Configure SDK (same as above)
        SDKConfig.setOptions(options);

        // Fetch single match
        FixtureMatch match = MatchesClient.fetchMatch("123456");

        System.out.println("Match: " + match.getName());
        System.out.println("Status: " + match.getStatus());
        System.out.println("Scheduled: " + match.getScheduledAt());
        System.out.println("League: " + match.getLeague().getName());
        System.out.println("Tournament: " + match.getTournament().getName());

        match.getOpponents().forEach(opp -> {
            System.out.println("  - " + opp.getOpponent().getName());
        });
    }
}
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `apiToken` | String | **Required** | REST API authentication token |
| `companyId` | long | **Required** | Your PandaScore account ID |
| `email` | String | **Required** | Your account email |
| `password` | String | **Required** | Your account password |
| `feedHost` | String | `trading-feed.pandascore.co` | RabbitMQ feed hostname |
| `apiBaseUrl` | String | `https://api.pandascore.co/betting/matches` | REST API base URL |
| `queueBindings` | List | **Required** | Queue declarations (at least one) |
| `americanOdds` | boolean | `false` | Compute American odds format |
| `fractionalOdds` | boolean | `false` | Compute fractional odds format |
| `recoverOnReconnect` | boolean | `true` | Auto-recover markets on reconnection |
| `alwaysLogPayload` | boolean | `false` | Log all payloads at INFO level |

## Routing Keys

Subscribe to specific message types using routing keys:

- `#` - All messages
- `pandascore.markets.#` - All market updates
- `pandascore.fixtures.#` - All fixture updates
- `pandascore.markets.*.*.created` - Only market creation events
- `pandascore.fixtures.*.*.updated` - Only fixture update events

Example:
```java
.queueBinding(
    SDKOptions.QueueBinding.builder()
        .queueName("my-markets-queue")
        .routingKey("pandascore.markets.#")  // Only markets
        .build()
)
```

## Message Types

### Markets Message
Contains odds and selections for markets.

**Fields:**
- `type`: "markets"
- `action`: "created", "odds_changed", "suspended", "settled", etc.
- `matchId`: Match ID
- `markets`: List of market objects
  - `id`, `name`, `status`, `template`
  - `selections`: List of selections
    - `name`, `oddsDecimal`, `oddsDecimalWithOverround`
    - `probability`, `probabilityWithOverround`

### Fixture Message
Contains match/fixture information.

**Fields:**
- `type`: "fixture"
- `action`: "created", "started", "finished", "settled", etc.
- `match`: Full match object
  - `id`, `name`, `status`, `scheduledAt`
  - `league`, `tournament`, `serie`
  - `opponents`, `games`, `results`

## Running the Built-in Example

```bash
# Build the project
./gradlew build

# Run the example
./gradlew run

# Or run directly
java -cp build/libs/sdk.jar com.pandascore.sdk.examples.FeedConsole
```

## Troubleshooting

### Connection Issues
- Verify credentials are correct
- Check firewall allows AMQPS (port 5671)
- Ensure `feedHost` is reachable

### No Messages Received
- Verify routing key is correct (`#` for all messages)
- Check queue name doesn't conflict with existing queues
- Ensure there are active matches with markets

### Parsing Errors
- Make sure Jackson dependencies are included
- Enable debug logging to see raw JSON

## Next Steps

- Check out `/src/main/java/com/pandascore/sdk/examples/FeedConsole.java` for a complete example
- Read `IMPROVEMENTS_NEEDED.md` for full data model documentation
- Explore all model classes in `src/main/java/com/pandascore/sdk/model/`

## Support

For issues or questions:
- GitHub Issues: https://github.com/jernej19/java-sdk/issues
- API Documentation: https://developers.pandascore.co
