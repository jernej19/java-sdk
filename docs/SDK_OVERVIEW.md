# SDK Overview

This document explains the main packages and classes of the PandaScore Java SDK and how they work together.

## Packages

### `config`
Holds global configuration:
- **`SDKOptions`** – builder-style POJO describing runtime options such as credentials, feed host and queue bindings. Includes a nested `QueueBinding` class.
- **`SDKConfig`** – singleton holder for the configured `SDKOptions`. You must call `SDKConfig.setOptions` once at startup before using other SDK APIs.

### `events`
Provides utilities for connection state and heartbeat tracking:
- **`ConnectionEvent`** – typed event emitted on disconnection (code 100) or reconnection (code 101). Reconnection events include `RecoveryData` with recovered markets and matches.
- **`EventHandler`** – schedules heartbeat checks every 10 seconds, tracks a missed-beat counter (disconnects after 3 consecutive misses), and triggers automatic market/match recovery via `MatchesClient`. Heartbeats are identified by messages with an `at` field and no `type` field.

### `http`
HTTP client helpers:
- **`MatchesClient`** – static methods `recoverMarkets(since)` (returns matches with their markets) and `fetchMatchesRange(start, end)` for calling PandaScore recovery endpoints. Requires that `SDKConfig` has been initialised.

### `model`
Typed domain objects under `model.feed.*` representing the JSON payloads emitted by the feed (fixtures, markets, scoreboard, etc.). These classes are used to deserialize events and recovery responses.

### `rmq`
RabbitMQ integration:
- **`RabbitMQFeed`** – connects to the AMQP feed using the settings from `SDKOptions`, consumes messages from declared queues, handles reconnection with exponential backoff and exposes `connect(Consumer)` and `close()`. Supports up to 10 concurrent connections with up to 10 queues each.

## Typical Initialisation Flow

1. Create an `SDKOptions` instance using its builder and list any queue bindings.
2. Call `SDKConfig.setOptions(options)` once to make the options available globally.
3. Instantiate an `EventHandler` to receive `ConnectionEvent` callbacks (code 100 for disconnection, code 101 for reconnection).
4. Create a `RabbitMQFeed` with that handler and invoke `connect()` providing a consumer that processes each JSON event.
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
- `RabbitMQFeed.connect(Consumer)` and `RabbitMQFeed.close()`.
- `RabbitMQFeed.getActiveConnectionCount()` — monitor current connection count.
- `RabbitMQFeed.isRecoverOnReconnect()` — check per-connection recovery setting.
- `EventHandler.heartbeat()`, `EventHandler.resetTimer()`, `EventHandler.handleDisconnection()` and `EventHandler.close()`.
- `MatchesClient.recoverMarkets(String)` and `MatchesClient.fetchMatchesRange(String, String)`.

These building blocks allow you to reliably consume feed updates with automatic recovery and strongly typed domain objects.
