# Changelog

All notable changes to the PandaScore Java SDK will be documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-04-03

### Added
- Initial release of the PandaScore Java SDK.
- **AMQPS streaming** via RabbitMQ with automatic reconnection (exponential backoff with jitter:
  base `attempt * 5s`, max 60s, plus random jitter up to half the base).
- **`FeedListener` interface** for typed message callbacks — `onMarkets(MarketsMessage)`,
  `onFixture(FixtureMessage)`, `onScoreboard(JsonNode, String)`, `onUnknown(JsonNode)`.
  Override only the methods you need; defaults are no-ops.
- **`TypedFeedAdapter`** wraps a `FeedListener` into a `Consumer<Object>`, auto-deserializes
  incoming JSON by `type` field, and dispatches to the matching listener method.
- **`RabbitMQFeed.connect(FeedListener)`** for typed consumption, and `connect(Consumer)` for
  raw `JsonNode` access.
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
- **`RecoveryData.isComplete()`** flag — returns `true` if both recovery API calls succeeded,
  `false` if recovery was partial or failed.
- **`EventHandler`** with heartbeat monitoring and thread-safe state management — disconnection
  detected after 3 consecutive missed 10-second heartbeats; reconnection triggered on first
  heartbeat after AMQP transport recovers.
- **`ConnectionEvent`** with codes `100` (disconnection) and `101` (reconnection + recovery data).
- **HTTP client** (`MatchesClient`) for recovery APIs with 30s connect / 60s read timeouts.
- **`JsonMapperFactory`** — shared `ObjectMapper` factory (`NON_NULL` + `JavaTimeModule`) for
  consistent JSON configuration across all SDK components.
- **Message models** for markets, fixtures, and game-specific scoreboards
  (CS, Dota 2, LoL, Valorant, eSoccer, eBasketball, eHockey, eTennis).
- **SLF4J + Logback** async logging with MDC context tags (`session`, `customerId`, `feed`,
  `messageType`, `operation`, `routingKey`).
- **`SDKOptions`** builder with full configuration reference (token, credentials, queue bindings,
  odds format flags, recovery toggle, prefetch count).
- **`BasicExample.java`** and **`MultiConnectionExample.java`** examples.
