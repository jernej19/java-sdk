# Java SDK Quick Start Guide

## Step-by-Step: Create a Maven Project That Uses the SDK

This guide walks you through creating a new Maven project from scratch, adding the SDK dependency, and running a working example.

---

### Step 1: Create the Project Structure

Create a new directory for your project and set up the standard Maven folder structure:

```bash
mkdir my-pandascore-app
cd my-pandascore-app
mkdir -p src/main/java/com/example
```

> **Important**: Maven requires Java files to be under `src/main/java/` followed by directories matching the package name. For example, `package com.example;` must be in `src/main/java/com/example/`.

---

### Step 2: Create `pom.xml`

Create a `pom.xml` file in the project root (`my-pandascore-app/pom.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-pandascore-app</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <!-- GitHub Packages repository for the PandaScore SDK -->
    <repositories>
        <repository>
            <id>github-pandascore</id>
            <url>https://maven.pkg.github.com/PandaScore/pandascore-sdk-java</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>co.pandascore</groupId>
            <artifactId>pandascore-sdk-java</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Required to run examples with: mvn exec:java -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### Step 3: Configure GitHub Packages Authentication

GitHub Packages requires authentication to download packages. Create or edit `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>github-pandascore</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_PERSONAL_ACCESS_TOKEN</password>
        </server>
    </servers>
</settings>
```

**To generate a Personal Access Token:**
1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Select the `read:packages` scope
4. Copy the token and paste it as the `<password>` above

> **Important**: The `<id>` in `settings.xml` must match the `<id>` in your `pom.xml` `<repository>` block. Both must be `github-pandascore`.

---

### Step 4: Create Your Application

Create `src/main/java/com/example/App.java`:

```java
package com.example;

import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.ConnectionEvent;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.rmq.RabbitMQFeed;
import com.fasterxml.jackson.databind.JsonNode;

public class App {
    public static void main(String[] args) throws Exception {

        // 1. Configure SDK with your PandaScore credentials
        SDKOptions options = SDKOptions.builder()
            .apiToken("YOUR_API_TOKEN")
            .companyId(12345)
            .email("your-email@example.com")
            .password("your-password")
            .queueBinding(
                SDKOptions.QueueBinding.builder()
                    .queueName("your-queue-name")
                    .routingKey("#")  // "#" = receive all messages
                    .build()
            )
            .build();

        SDKConfig.setOptions(options);

        // 2. Set up event handler for disconnection/reconnection
        EventHandler eventHandler = new EventHandler(event -> {
            if (event.getCode() == ConnectionEvent.CODE_DISCONNECTION) {
                System.out.println("[EVENT] Disconnected! Suspend your markets.");
            } else if (event.getCode() == ConnectionEvent.CODE_RECONNECTION) {
                ConnectionEvent.RecoveryData data = event.getRecoveryData();
                System.out.println("[EVENT] Reconnected! Recovered "
                    + data.getMarkets().size() + " markets, "
                    + data.getMatches().size() + " matches.");
                if (!data.isComplete()) {
                    System.out.println("[EVENT] Warning: recovery was partial.");
                }
            }
        });

        // 3. Connect and process messages
        RabbitMQFeed feed = new RabbitMQFeed(eventHandler);

        feed.connect(message -> {
            JsonNode json = (JsonNode) message;
            String type = json.has("type") ? json.get("type").asText() : "heartbeat";

            switch (type) {
                case "markets":
                    System.out.println("[MARKETS] Match " + json.get("match_id")
                        + " — action: " + json.get("action"));
                    break;
                case "fixture":
                    System.out.println("[FIXTURE] Match " + json.get("match_id")
                        + " — action: " + json.get("action"));
                    break;
                case "scoreboard":
                    System.out.println("[SCOREBOARD] Match " + json.get("match_id"));
                    break;
                default:
                    break;
            }
        });

        System.out.println("Connected! Receiving live updates... (Ctrl+C to stop)");
        Thread.currentThread().join();
    }
}
```

---

### Step 5: Compile and Run

```bash
# Compile (this downloads the SDK from GitHub Packages on first run)
mvn compile

# Run the application
mvn exec:java -Dexec.mainClass="com.example.App"
```

If `mvn compile` succeeds, the SDK is correctly installed and all dependencies are resolved.

---

## Using Typed Callbacks (FeedListener)

Instead of parsing raw JSON, you can use the `FeedListener` interface for automatic deserialization. Override only the methods you need:

Create `src/main/java/com/example/TypedApp.java`:

```java
package com.example;

import com.pandascore.sdk.FeedListener;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.events.ConnectionEvent;
import com.pandascore.sdk.events.EventHandler;
import com.pandascore.sdk.rmq.RabbitMQFeed;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;
import com.pandascore.sdk.model.feed.fixtures.FixtureMessage;
import com.fasterxml.jackson.databind.JsonNode;

public class TypedApp {
    public static void main(String[] args) throws Exception {

        // Configure SDK (same as basic example)
        SDKOptions options = SDKOptions.builder()
            .apiToken("YOUR_API_TOKEN")
            .companyId(12345)
            .email("your-email@example.com")
            .password("your-password")
            .queueBinding(
                SDKOptions.QueueBinding.builder()
                    .queueName("your-queue-name")
                    .routingKey("#")
                    .build()
            )
            .build();

        SDKConfig.setOptions(options);

        EventHandler eventHandler = new EventHandler(event -> {
            if (event.getCode() == ConnectionEvent.CODE_DISCONNECTION) {
                System.out.println("Disconnected! Suspend markets.");
            } else {
                ConnectionEvent.RecoveryData data = event.getRecoveryData();
                System.out.println("Reconnected! Recovered "
                    + data.getMarkets().size() + " markets.");
                if (!data.isComplete()) {
                    System.out.println("Warning: recovery was partial.");
                }
            }
        });

        // Connect with typed listener — no manual JSON parsing needed
        RabbitMQFeed feed = new RabbitMQFeed(eventHandler);

        feed.connect(new FeedListener() {
            @Override
            public void onMarkets(MarketsMessage markets) {
                System.out.println("[MARKETS] Match " + markets.getMatchId()
                    + " — " + markets.getAction());
                markets.getMarkets().forEach(market -> {
                    System.out.println("  Market: " + market.getName());
                    market.getSelections().forEach(sel -> {
                        System.out.printf("    %s: %.2f%n",
                            sel.getName(), sel.getOddsDecimalWithOverround());
                    });
                });
            }

            @Override
            public void onFixture(FixtureMessage fixture) {
                System.out.println("[FIXTURE] Match " + fixture.getMatchId()
                    + " — " + fixture.getAction());
            }

            @Override
            public void onScoreboard(JsonNode raw, String scoreboardType) {
                System.out.println("[SCOREBOARD] Type: " + scoreboardType
                    + " — Match " + raw.get("match_id"));
            }
        });

        System.out.println("Connected! Receiving typed updates... (Ctrl+C to stop)");
        Thread.currentThread().join();
    }
}
```

Run it:
```bash
mvn compile exec:java -Dexec.mainClass="com.example.TypedApp"
```

---

## Multiple Connections

The SDK supports up to 10 concurrent connections, each with its own queues. This is useful for splitting traffic (e.g., markets on one connection, fixtures on another).

```java
// Connection 1: markets — with recovery enabled
List<SDKOptions.QueueBinding> marketsBindings = List.of(
    SDKOptions.QueueBinding.builder()
        .queueName("markets-queue")
        .routingKey("*.*.*.markets.#")
        .build()
);
EventHandler marketsHandler = new EventHandler(event -> { /* ... */ });
RabbitMQFeed marketsFeed = new RabbitMQFeed(marketsHandler, marketsBindings, true);
marketsFeed.connect(msg -> { /* process markets */ });

// Connection 2: fixtures — recovery disabled (connection 1 handles it)
List<SDKOptions.QueueBinding> fixtureBindings = List.of(
    SDKOptions.QueueBinding.builder()
        .queueName("fixtures-queue")
        .routingKey("*.*.*.fixture.#")
        .build()
);
EventHandler fixturesHandler = new EventHandler(event -> { /* ... */ });
RabbitMQFeed fixturesFeed = new RabbitMQFeed(fixturesHandler, fixtureBindings, false);
fixturesFeed.connect(msg -> { /* process fixtures */ });
```

**Key points:**
- Each connection has its own `EventHandler` and `RabbitMQFeed`
- Set `recoverOnReconnect` to `true` on **only one** connection to avoid redundant recovery API calls
- Max 10 connections, max 10 queues per connection
- See [MultiConnectionExample.java](src/main/java/com/pandascore/sdk/examples/MultiConnectionExample.java) for a full example

---

## Gradle Setup (Alternative to Maven)

If you use Gradle instead of Maven, add to `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/PandaScore/pandascore-sdk-java")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("co.pandascore:pandascore-sdk-java:1.0.0")
}
```

Set `GITHUB_USERNAME` and `GITHUB_TOKEN` as environment variables.

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| `Could not find artifact` | Package not accessible | Check that your GitHub account has read access to the repository and your PAT has `read:packages` scope |
| `resolution is not reattempted` | Maven cached a previous failure | Run `rm -rf ~/.m2/repository/co/pandascore` then `mvn compile -U` |
| `401 Unauthorized` | Wrong credentials | Verify `~/.m2/settings.xml` has the correct username and PAT, and the `<id>` matches the `<repository>` id in `pom.xml` |
| `class X is public, should be declared in file named X.java` | File name doesn't match class name | The Java file name must exactly match the `public class` name inside it |
| `No sources to compile` | Wrong directory structure | Java files must be under `src/main/java/` with directories matching the package (e.g., `package com.example;` → `src/main/java/com/example/`) |
| `ClassNotFoundException` when running | Code not compiled | Run `mvn compile` before `mvn exec:java` |

---

## Next Steps

- Read the main [README.md](README.md) for comprehensive SDK documentation
- See `src/main/java/com/pandascore/sdk/examples/` for more advanced examples
- Explore model classes in `src/main/java/com/pandascore/sdk/model/`
