# SDK Overview

This document explains the main packages and classes of the PandaScore Java SDK and how they work together.

## Packages

### `config`
Holds global configuration:
- **`SDKOptions`** – builder-style POJO describing runtime options such as credentials, feed host and queue bindings. Includes a nested `QueueBinding` class.
- **`SDKConfig`** – thread-safe singleton holder for the configured `SDKOptions`. You must call `SDKConfig.setOptions` once at startup before using other SDK APIs.
- **`JsonMapperFactory`** – shared factory for creating pre-configured `ObjectMapper` instances (NON_NULL serialization, JavaTimeModule). Used internally by `RabbitMQFeed` and `MatchesClient`.

### `events`
Provides utilities for connection state and heartbeat tracking:
- **`ConnectionEvent`** – typed event emitted on disconnection (code 100) or reconnection (code 101). Reconnection events include `RecoveryData` with recovered markets, matches, and a `isComplete()` flag indicating whether both recovery API calls succeeded.
- **`EventHandler`** – schedules heartbeat checks every 10 seconds, tracks a missed-beat counter (disconnects after 3 consecutive misses), and triggers automatic market/match recovery via `MatchesClient`. Uses a dedicated state lock to prevent race conditions between heartbeat, disconnection, and recovery operations. Heartbeats are identified by messages with an `at` field and no `type` field.

### `http`
HTTP client helpers:
- **`MatchesClient`** – static methods `recoverMarkets(since)` (returns matches with their markets) and `fetchMatchesRange(start, end)` for calling PandaScore recovery endpoints. Requires that `SDKConfig` has been initialised.

### `model`
Typed domain objects under `model.feed.*` representing the JSON payloads emitted by the feed (fixtures, markets, scoreboard, etc.). These classes are used to deserialize events and recovery responses.

### Top-level (`com.pandascore.sdk`)
Typed message callback support:
- **`FeedListener`** – interface with default no-op methods: `onMarkets(MarketsMessage)`, `onFixture(FixtureMessage)`, `onScoreboard(JsonNode, String)`, `onUnknown(JsonNode)`. Override only the callbacks you need.
- **`TypedFeedAdapter`** – `Consumer<Object>` implementation that wraps a `FeedListener`, inspects the `type` field, deserializes to the appropriate model, and dispatches. Falls back to `onUnknown` on deserialization errors.

### `rmq`
RabbitMQ integration:
- **`RabbitMQFeed`** – connects to the AMQP feed using the settings from `SDKOptions`, consumes messages from declared queues, handles reconnection with jitter-based exponential backoff, and exposes `connect(Consumer)`, `connect(FeedListener)`, and `close()`. The `connect()` method is synchronized to prevent races between manual calls and the reconnect scheduler. Supports up to 10 concurrent connections with up to 10 queues each.

## Typical Initialisation Flow

1. Create an `SDKOptions` instance using its builder and list any queue bindings.
2. Call `SDKConfig.setOptions(options)` once to make the options available globally.
3. Instantiate an `EventHandler` to receive `ConnectionEvent` callbacks (code 100 for disconnection, code 101 for reconnection).
4. Create a `RabbitMQFeed` with that handler and invoke `connect(FeedListener)` (typed) or `connect(Consumer)` (raw JSON).
5. Use `MatchesClient` for explicit recovery or fetching of matches when needed.

## Multiple Connections

The SDK supports up to 10 concurrent `RabbitMQFeed` instances, each with its own queue bindings and independent `EventHandler`. This allows splitting message types across dedicated connections.

**Constructors:**
- `RabbitMQFeed(handler)` — uses queue bindings and recovery setting from global `SDKOptions`
- `RabbitMQFeed(handler, queueBindings)` — per-connection queue bindings, global recovery setting
- `RabbitMQFeed(handler, queueBindings, recoverOnReconnect)` — per-connection queue bindings and per-connection recovery flag (`true`/`false`/`null` for global default)

**Per-connection recovery:**
Each connection independently detects disconnection and triggers recovery. When running multiple connections, set `recoverOnReconnect=false` on all connections except one to avoid redundant recovery API calls. The designated recovery connection will call `recoverMarkets` and `fetchMatchesRange`; other connections simply reconnect and resume consuming.

**Limits:**
- Max 10 concurrent connections (hard-enforced, throws `IllegalStateException`)
- Max 10 queue bindings per connection (validated at construction time)

## Key Public API

The main public methods customers typically interact with are:
- `SDKConfig.setOptions(SDKOptions)` and `SDKConfig.getInstance().getOptions()`.
- `RabbitMQFeed.connect(FeedListener)` — typed callback interface (recommended).
- `RabbitMQFeed.connect(Consumer)` — raw `JsonNode` consumer (advanced).
- `RabbitMQFeed.close()` — clean shutdown with executor termination.
- `RabbitMQFeed.getActiveConnectionCount()` — monitor current connection count.
- `RabbitMQFeed.isRecoverOnReconnect()` — check per-connection recovery setting.
- `ConnectionEvent.RecoveryData.isComplete()` — check if recovery was fully successful.
- `EventHandler.heartbeat()`, `EventHandler.resetTimer()`, `EventHandler.handleDisconnection()` and `EventHandler.close()`.
- `MatchesClient.recoverMarkets(String)` and `MatchesClient.fetchMatchesRange(String, String)`.
- `JsonMapperFactory.create()` — obtain a pre-configured `ObjectMapper` for manual deserialization.

These building blocks allow you to reliably consume feed updates with automatic recovery and strongly typed domain objects.
