# Changelog

All notable changes to the PandaScore Java SDK will be documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-03-31

### Added
- Initial release of the PandaScore Java SDK.
- **AMQPS streaming** via RabbitMQ with automatic reconnection (exponential backoff: 5s x attempt, max 60s).
- **Multiple concurrent connections**: Up to 10 `RabbitMQFeed` instances simultaneously, each with
  its own queue bindings (up to 10 per connection). Connection limit is hard-enforced via
  `IllegalStateException`.
- **Per-connection queue bindings**: `RabbitMQFeed(handler, queueBindings)` and
  `RabbitMQFeed(handler, queueBindings, recoverOnReconnect)` constructors allow different
  connections to consume from different queues.
- **Per-connection recovery control**: `recoverOnReconnect` flag can be set per-connection
  (`true`/`false`/`null`). Set to `false` on all but one connection to avoid redundant recovery
  API calls.
- **Recovery mode**: Buffers incoming messages during reconnection, then fetches missed data via
  REST (`recoverMarkets`, `fetchMatchesRange`) before replaying the buffer.
- **`EventHandler`** with heartbeat monitoring — disconnection detected after 3 consecutive missed
  10-second heartbeats; reconnection triggered on first heartbeat after AMQP transport recovers.
- **`ConnectionEvent`** with codes `100` (disconnection) and `101` (reconnection + recovery data).
- **HTTP client** (`MatchesClient`) for recovery APIs with 30s connect / 60s read timeouts.
- **Message models** for markets, fixtures, and game-specific scoreboards
  (CS, Dota 2, LoL, Valorant, eSoccer, eBasketball, eHockey).
- **SLF4J + Logback** async logging with MDC context tags (`session`, `customerId`, `feed`,
  `messageType`, `operation`, `routingKey`).
- **`SDKOptions`** builder with full configuration reference (token, credentials, queue bindings,
  odds format flags, recovery toggle).
- **`BasicExample.java`** and **`MultiConnectionExample.java`** examples.
