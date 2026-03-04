# CLAUDE.md — PandaScore Java SDK

This file provides guidance for AI assistants working in this repository.

## Project Overview

This is the **PandaScore Java SDK** — a production-ready client library for consuming PandaScore's real-time esports betting data feed. It provides:

- **AMQPS streaming** via RabbitMQ for live markets, fixtures, and scoreboards
- **Automatic reconnection** with exponential backoff
- **Recovery mode** that buffers incoming messages during reconnection then fetches missed data via REST
- **HTTP client** for recovery APIs (recover markets, fetch match ranges, fetch markets)

The project artifact name is `pandascore-sdk-java` (see `settings.gradle.kts`).

---

## Repository Structure

```
java-sdk/
├── build.gradle.kts                  # Gradle build (Kotlin DSL)
├── settings.gradle.kts               # Root project name
├── gradlew / gradlew.bat             # Gradle wrapper
├── src/
│   ├── main/java/com/pandascore/sdk/
│   │   ├── config/
│   │   │   ├── SDKConfig.java        # Singleton configuration holder
│   │   │   └── SDKOptions.java       # Builder-pattern options + QueueBinding
│   │   ├── events/
│   │   │   ├── ConnectionEvent.java  # Disconnection (100) / reconnection (101) events
│   │   │   └── EventHandler.java     # Heartbeat monitor + recovery orchestration
│   │   ├── http/
│   │   │   └── MatchesClient.java    # Static HTTP methods for recovery APIs
│   │   ├── rmq/
│   │   │   └── RabbitMQFeed.java     # AMQPS connection, message routing, recovery buffering
│   │   ├── model/feed/
│   │   │   ├── markets/              # MarketsMessage, MarketsMessageMarket, etc.
│   │   │   ├── fixtures/             # FixtureMessage, FixtureMatch, League, Player, etc.
│   │   │   └── scoreboard/           # ScoreboardCs, ScoreboardDota2, ScoreboardLol, etc.
│   │   └── examples/
│   │       └── BasicExample.java     # Runnable entry point demonstrating all message types
│   ├── test/java/com/pandascore/sdk/
│   │   ├── events/
│   │   │   └── EventHandlerTest.java
│   │   └── rmq/
│   │       └── HeartbeatDetectionTest.java
│   └── main/resources/
│       └── logback.xml               # Async, rolling file appenders with MDC
├── docs/
│   └── SDK_OVERVIEW.md
├── README.md
├── QUICKSTART.md
└── AUDIT-disconnection-logic.md      # Documents differences from TypeScript SDK reference
```

---

## Build System

**Tool**: Gradle with Kotlin DSL (`build.gradle.kts`)
**Java**: Standard Java plugin + Application plugin

### Common Commands

```bash
./gradlew build          # Compile all sources and run all tests
./gradlew test           # Run tests only
./gradlew run            # Run BasicExample (main class)
./gradlew javadoc        # Generate Javadoc HTML
./gradlew javadocJar     # Build Javadoc JAR
./gradlew jar            # Build library JAR
```

Tests use JUnit 5 — the build configures `useJUnitPlatform()`.

---

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `com.rabbitmq:amqp-client` | 5.20.0 | AMQPS connection |
| `com.fasterxml.jackson.core:jackson-databind` | 2.17.1 | JSON parsing |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | 2.17.1 | Java Time support |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP client |
| `com.squareup.okhttp3:logging-interceptor` | 4.12.0 | HTTP request/response logging |
| `org.slf4j:slf4j-api` | 2.0.13 | Logging API |
| `ch.qos.logback:logback-classic` | 1.5.6 | Logging implementation (runtime) |
| `org.projectlombok:lombok` | 1.18.32 | Code generation (`@Data`, `@Builder`) |

**Test-only**:
- `org.junit.jupiter:junit-jupiter` 5.10.2
- `org.mockito:mockito-core` + `mockito-junit-jupiter` 5.11.0
- `com.squareup.okhttp3:mockwebserver` 4.12.0

---

## Core Architecture

### Initialization Flow

```java
// 1. Build and register options (must be called before any SDK usage)
SDKOptions options = SDKOptions.builder()
    .apiToken("...")
    .companyId(12345)
    .email("...")
    .password("...")
    .queueBinding(SDKOptions.QueueBinding.builder()
        .queueName("my-queue")
        .routingKey("#")   // # = all messages; use patterns to filter
        .build())
    .build();
SDKConfig.setOptions(options);

// 2. Create event handler for disconnection/reconnection events
EventHandler handler = new EventHandler(event -> { /* handle events */ });

// 3. Connect and start consuming
RabbitMQFeed feed = new RabbitMQFeed(handler);
feed.connect(message -> { /* handle JsonNode messages */ });
```

### Message Processing Flow

1. `RabbitMQFeed.connect()` establishes AMQPS connection and calls `establish()` then `startConsumers()`
2. Each incoming message is checked with `isHeartbeatMessage(json)`:
   - **Heartbeat**: `json.has("at") && !json.has("type")` — calls `handler.heartbeat()`
   - **Business event**: dispatched to `sink.accept(json)` (or buffered if in recovery)
3. Routing key format: `...<eventType>.<eventId>.<action>` — last 4/3/1 segments identify event
4. Message ack/nack is handled manually (no auto-ack)

### Disconnection & Recovery Flow

```
Missed 3 heartbeats (30s) OR AMQP shutdown
    → EventHandler.handleDisconnection()
    → ConnectionEvent(code=100) emitted to application

AMQP reconnect (automatic, exponential backoff: 5s × attempt, max 60s)
    → First real heartbeat received
    → EventHandler.heartbeat() triggers recovery:
        1. feed.startRecovery()  ← buffer incoming messages
        2. MatchesClient.recoverMarkets(downAt)
        3. MatchesClient.fetchMatchesRange(downAt, now)
        4. feed.endRecovery()   ← drain buffer to sink
        5. ConnectionEvent(code=101) emitted with RecoveryData
```

**Key constants** (`EventHandler`):
- `HEARTBEAT_INTERVAL = Duration.ofSeconds(10)`
- `MAX_MISSED_COUNT = 3` (triggers disconnection after ~30s of silence)

**Connection event codes** (`ConnectionEvent`):
- `CODE_DISCONNECTION = 100`
- `CODE_RECONNECTION = 101`

---

## Package Conventions

### `com.pandascore.sdk.config`

- `SDKConfig` is a **singleton** — call `SDKConfig.setOptions(opts)` once at startup, then `SDKConfig.getInstance().getOptions()` anywhere.
- `SDKOptions` uses **Lombok `@Builder`** — all fields are immutable. Use `.builder()...build()`.
- `QueueBinding` is a nested `@Data @Builder` class inside `SDKOptions`.
- `SDKOptions.validate()` is called automatically by `SDKConfig.setOptions()`.

### `com.pandascore.sdk.events`

- `EventHandler` implements `AutoCloseable` — suitable for try-with-resources.
- Internal fields `disconnected`, `missedHeartbeats`, `lastBeat` are `volatile` for thread safety.
- Package-private accessors `isDisconnected()` and `getMissedHeartbeats()` exist for testing via reflection — do not make them public.
- `setFeed(RabbitMQFeed)` must be called before recovery can work (done internally by `RabbitMQFeed.connect()`).

### `com.pandascore.sdk.http`

- `MatchesClient` is a **static utility class** — no instantiation (`private` constructor).
- All methods require `SDKConfig.setOptions()` to have been called first.
- HTTP timeouts: connect=30s, read=60s, write=30s.
- Recovery endpoints:
  - `recoverMarkets(since)` → `GET /recover_markets/{since}?token=...`
  - `fetchMatchesRange(start, end)` → `GET /?range[modified_at]=...&filter[booked]=true&token=...`
  - `fetchMatch(id)` → `GET /{id}?token=...`
  - `fetchMarkets(matchId)` → `GET /{matchId}/markets?token=...`

### `com.pandascore.sdk.rmq`

- `RabbitMQFeed` implements `AutoCloseable` — always close in finally or try-with-resources.
- `automaticRecoveryEnabled = false` on the AMQP factory — reconnection is handled manually.
- `recovering` (volatile boolean) and `recoveryBuffer` (ConcurrentLinkedQueue) guard the recovery window.
- `startRecovery()` / `endRecovery()` are public and called by `EventHandler`.

### `com.pandascore.sdk.model`

- All model classes use `@Data` (Lombok) + `@JsonIgnoreProperties(ignoreUnknown = true)` (Jackson).
- Field names use `@JsonProperty` for snake_case JSON → camelCase Java mapping.
- Models are organized under `model/feed/{markets,fixtures,scoreboard}`.
- Scoreboard models are game-specific: `ScoreboardCs`, `ScoreboardDota2`, `ScoreboardLol`, `ScoreboardValorant`, `ScoreboardEsoccer`, `ScoreboardEbasketball`, `ScoreboardEhockey`.

---

## Logging

Logging uses **SLF4J + Logback** with async file appenders and MDC context tags.

**Log files** (see `src/main/resources/logback.xml`):
- `logs/sdk-debug.log` — DEBUG level
- `logs/sdk-info.log` — INFO level
- `logs/sdk-warn.log` — WARN and above
- Rolling: time-based, 30-day history

**MDC tags used throughout the codebase**:

| Tag | Meaning |
|---|---|
| `session` | Session identifier |
| `customerId` | Company/account ID |
| `feed` | Current message type or feed name |
| `messageType` | Raw `type` field from JSON |
| `operation` | Current operation (`connect`, `reconnect`, `disconnection`, `recoverMarkets`, etc.) |
| `routingKey` | AMQP routing key of the current message |

Always set MDC tags before log calls and remove them in `finally` blocks. Background tasks capture `MDC.getCopyOfContextMap()` and restore it at task start.

---

## Testing Conventions

- **Framework**: JUnit 5 Jupiter + Mockito
- **Reflection for internals**: `EventHandlerTest` uses reflection to set private fields (e.g., `disconnected`, `missedHeartbeats`) to test state without exposing them publicly. This is intentional — do not add public setters for test-only purposes.
- **Thread safety in tests**: Use `CopyOnWriteArrayList` when capturing events emitted from background threads.
- **HTTP mocking**: Use OkHttp `MockWebServer` to mock recovery API responses without real network calls.
- **Test method naming**: Descriptive names like `testHeartbeatMessage_withAtField_noTypeField()`.

Run tests:
```bash
./gradlew test
```

---

## SDKOptions Configuration Reference

| Field | Type | Default | Required | Description |
|---|---|---|---|---|
| `apiToken` | String | — | Yes | REST API authentication token |
| `companyId` | long | — | Yes | Must be positive |
| `email` | String | — | Yes | AMQP login email |
| `password` | String | — | Yes | AMQP login password |
| `feedHost` | String | `trading-feed.pandascore.co` | No | AMQP broker hostname |
| `apiBaseUrl` | String | `https://api.pandascore.co/betting/matches` | No | REST base URL |
| `queueBindings` | List\<QueueBinding\> | — | Yes (non-empty) | Queue + routing key pairs |
| `alwaysLogPayload` | boolean | `false` | No | Log payloads at INFO (else DEBUG) |
| `americanOdds` | boolean | `false` | No | Include American odds fields |
| `fractionalOdds` | boolean | `false` | No | Include fractional odds fields |
| `recoverOnReconnect` | boolean | `true` | No | Auto-call recovery APIs on reconnect |

---

## Code Style & Conventions

- **Naming**: Classes = PascalCase, methods/fields = camelCase, constants = UPPER_SNAKE_CASE
- **Immutability**: All model classes and config objects are immutable (Lombok `@Data` + `@Builder`)
- **Concurrency**: Use `volatile` for cross-thread flags; `ConcurrentLinkedQueue` for thread-safe buffers; daemon threads for background executors
- **Error handling**: Catch-and-log with propagation for critical paths; nack-and-requeue for message processing failures
- **Javadoc**: All public classes and methods must have Javadoc (`-Xdoclint:none` suppresses strict warnings)
- **Jackson**: Configure `ObjectMapper` with `JavaTimeModule` and `NON_NULL` serialization inclusion
- **Resource cleanup**: All `AutoCloseable` resources should be closed — either try-with-resources or explicit `close()` in finally
- **No framework injection**: The SDK uses no DI framework; dependencies are passed via constructors

---

## Important Behavioral Details

1. **Heartbeat detection**: A message is a heartbeat if `json.has("at") && !json.has("type")`. This matches the TypeScript SDK convention.

2. **Recovery triggers on first real heartbeat** after reconnect — NOT immediately on AMQP reconnection. `resetTimer()` is called after AMQP reconnect to avoid spurious missed-beat counts.

3. **Message buffering**: During recovery, messages are buffered in `ConcurrentLinkedQueue<JsonNode>` and replayed after recovery APIs return, before the reconnection event fires.

4. **AMQP reconnection is manual**: `automaticRecoveryEnabled = false`. The SDK manages its own retry loop via `ScheduledExecutorService`.

5. **Exponential backoff formula**: `delay = Math.min(attempt * 5, 60)` seconds (5s, 10s, 15s, ... up to 60s).

6. **Exchange**: Always `"pandascore.feed"` (topic type, durable).

7. **Routing key structure**: `...<eventType>.<eventId>.<action>` — the consumer parses `parts[length-4]`, `parts[length-3]`, `parts[length-1]`.

8. **Connection limit warning**: A global `AtomicInteger` counter tracks active AMQP connections across all `RabbitMQFeed` instances. When the count reaches `MAX_CONNECTIONS` (10), the SDK logs a WARN-level message advising the user to close unused feeds. The SDK does **not** hard-block new connections — it only warns. `RabbitMQFeed.getActiveConnectionCount()` exposes the current count for monitoring. Old connections are automatically closed before reconnecting to prevent leaks.

---

## Known Differences from TypeScript SDK Reference

See `AUDIT-disconnection-logic.md` for a full audit. Key differences:

- **Recovery trigger**: Java fires recovery on first real heartbeat after AMQP reconnect; TypeScript fires on AMQP reconnect directly.
- **Disconnection detection**: Java checks missed heartbeats on a fixed 10s schedule; TypeScript uses a rolling window.

These differences are documented and assessed as acceptable for production use.
