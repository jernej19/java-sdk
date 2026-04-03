# Changelog

All notable changes to the PandaScore Java SDK will be documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-04-03

### Added
- **`FeedListener` interface** for typed message callbacks — `onMarkets(MarketsMessage)`,
  `onFixture(FixtureMessage)`, `onScoreboard(JsonNode, String)`, `onUnknown(JsonNode)`.
  Override only the methods you need; defaults are no-ops.
- **`TypedFeedAdapter`** — wraps a `FeedListener` into a `Consumer<Object>`, auto-deserializes
  incoming JSON by `type` field, and dispatches to the matching listener method. Falls back to
  `onUnknown` on deserialization errors.
- **`RabbitMQFeed.connect(FeedListener)`** overload for typed consumption without manual JSON parsing.
- **`RecoveryData.isComplete()`** flag on reconnection events — returns `true` if both recovery
  API calls succeeded, `false` if recovery was partial or failed.
- **`JsonMapperFactory`** — shared `ObjectMapper` factory (`NON_NULL` + `JavaTimeModule`) used
  by `RabbitMQFeed`, `MatchesClient`, and `TypedFeedAdapter`.
- **Reconnection jitter** — randomized delay added to exponential backoff to prevent thundering
  herd in multi-client deployments.

### Fixed
- **NPE in `MatchesClient.fetchMarkets()`** when API returns `null` for `games` or `markets` lists.
  Now returns empty list instead of crashing during recovery.
- **`SDKConfig` singleton thread safety** — `instance` field is now `volatile`, ensuring visibility
  across threads.
- **Heartbeat race condition** — added `stateLock` to `EventHandler` to prevent concurrent
  `heartbeat()` calls from both triggering recovery simultaneously.
- **`connect()` race condition** — method is now `synchronized` to prevent the reconnect scheduler
  and manual calls from racing through `establish()`.
- **Reconnect task not cancellable** — `ScheduledFuture` is now stored and cancelled on `close()`.
- **Executor shutdown** — `awaitTermination(5s)` added to both `RabbitMQFeed.close()` and
  `EventHandler.shutdown()` to ensure background tasks complete before returning.
- **MDC context pollution** — `connect()` now saves and restores the full MDC context map instead
  of leaking keys like `session`, `customerId`, and `feed`.
- **Recovery status misreported** — the "Recovery complete" log now only appears when both API
  calls succeed. Partial failures are logged with a clear warning.
- **Missing `@JsonIgnoreProperties`** on all 8 scoreboard model classes (`ScoreboardCs`,
  `ScoreboardDota2`, `ScoreboardLol`, `ScoreboardValorant`, `ScoreboardEsoccer`,
  `ScoreboardEbasketball`, `ScoreboardEhockey`, `ScoreboardEtennis`). Without this annotation,
  any new field added to the feed would cause deserialization failures.

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
