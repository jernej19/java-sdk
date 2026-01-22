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

See [README.md](README.md#configuration-options) for all available configuration options.

## Routing Keys

Use `#` to subscribe to all messages (recommended):

```java
.queueBinding(
    SDKOptions.QueueBinding.builder()
        .queueName("my-queue")
        .routingKey("#")  // All messages
        .build()
)
```

See [README.md](README.md#queue-bindings) for routing key format details.

## Message Types

The SDK supports three message types:
- **Markets** - Odds and betting markets (`type: "markets"`)
- **Fixtures** - Match and tournament updates (`type: "fixture"`)
- **Scoreboards** - Live scores and game state (`type: "scoreboard"`)

See [README.md](README.md#data-models) for complete data model documentation.

## Running the Built-in Example

```bash
# Build the project
./gradlew build

# Run BasicExample
./gradlew run

# Or run directly
java -cp build/libs/sdk.jar com.pandascore.sdk.examples.BasicExample
```

## Troubleshooting

See [README.md](README.md#troubleshooting) for troubleshooting tips.

## Next Steps

- Check out `src/main/java/com/pandascore/sdk/examples/BasicExample.java` for a complete example
- Read the main [README.md](README.md) for comprehensive documentation
- Explore model classes in `src/main/java/com/pandascore/sdk/model/`
- Visit [API Documentation](https://pandaodds.readme.io/) for REST API reference
