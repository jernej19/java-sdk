# Changelog

All notable changes to the PandaScore Java SDK will be documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.0] - 2026-03-30

### Added
- **Multiple concurrent connections**: Customers can now create up to 10 `RabbitMQFeed` instances
  simultaneously, each with its own queue bindings (up to 10 per connection). The connection limit
  is hard-enforced — exceeding it throws `IllegalStateException`.
- **Per-connection queue bindings**: New `RabbitMQFeed(handler, queueBindings)` and
  `RabbitMQFeed(handler, queueBindings, recoverOnReconnect)` constructors allow different
  connections to consume from different queues.
- **Per-connection recovery control**: The `recoverOnReconnect` flag can now be set per-connection
  (`true`/`false`/`null`). When running multiple connections, set `recoverOnReconnect=false` on all
  but one connection to avoid redundant recovery API calls.
- **`MultiConnectionExample.java`**: New example demonstrating multi-connection setup with
  per-connection recovery.
- **`MAX_QUEUES_PER_CONNECTION` constant**: `SDKOptions.MAX_QUEUES_PER_CONNECTION = 10`, enforced
  in `SDKOptions.validate()`, `SDKConfig.setOptions()`, and `RabbitMQFeed` constructors.

### Changed
- **Connection limit enforcement**: The `MAX_CONNECTIONS = 10` limit is now a hard block
  (throws `IllegalStateException`) instead of a warning log.
- **`EventHandler` recovery check**: `heartbeat()` now reads the `recoverOnReconnect` flag from
  the associated `RabbitMQFeed` instance instead of the global `SDKConfig` singleton.

---

## [1.1.0] - 2026-03-06

### Added
- **Connection limit warning**: `RabbitMQFeed` now tracks active AMQP connections globally
  via a static `AtomicInteger`. When the count reaches 10 (`MAX_CONNECTIONS`), a WARN-level
  log is emitted advising the caller to close unused feeds. `RabbitMQFeed.getActiveConnectionCount()`
  exposes the current count for monitoring.
- **Heartbeat ready flag**: `EventHandler` no longer counts missed heartbeats until the AMQP
  connection is fully established. `checkHeartbeat()` is a no-op until `resetTimer()` is called
  (which happens inside `RabbitMQFeed.connect()` on success), preventing false disconnection
  events during the initial dial-up phase.
- **5-second heartbeat grace period**: A `HEARTBEAT_GRACE = 5s` buffer is applied before a
  heartbeat is counted as missed (`HEARTBEAT_INTERVAL + HEARTBEAT_GRACE = 15s` threshold).
  This absorbs normal network jitter without requiring any configuration change.

### Fixed
- **Connection leak in `RabbitMQFeed`**: Calling `connect()` on an already-connected feed,
  or reconnecting after a failure, previously orphaned the old AMQP connection without closing
  it. `establish()` now calls `closeExistingConnection()` before creating a new connection.

---

## [1.0.0] - 2026-03-01

### Added
- Initial release of the PandaScore Java SDK.
- AMQPS streaming via RabbitMQ with automatic reconnection (exponential backoff: 5s × attempt, max 60s).
- Recovery mode: buffers incoming messages during reconnection, then fetches missed data via REST
  (`recoverMarkets`, `fetchMatchesRange`) before replaying the buffer.
- `EventHandler` with heartbeat monitoring — disconnection detected after 3 consecutive missed
  10-second heartbeats; reconnection triggered on first heartbeat after AMQP transport recovers.
- `ConnectionEvent` with codes `100` (disconnection) and `101` (reconnection + recovery data).
- HTTP client (`MatchesClient`) for recovery APIs with 30s connect / 60s read timeouts.
- Message models for markets, fixtures, and game-specific scoreboards
  (CS, Dota 2, LoL, Valorant, eSoccer, eBasketball, eHockey).
- SLF4J + Logback async logging with MDC context tags (`session`, `customerId`, `feed`,
  `messageType`, `operation`, `routingKey`).
- `SDKOptions` builder with full configuration reference (token, credentials, queue bindings,
  odds format flags, recovery toggle).
