# Audit: Java SDK Disconnection Logic vs TypeScript SDK Reference

**Date**: 2026-02-24
**Scope**: Heartbeat monitoring, disconnection detection, recovery process

---

## Summary

| # | Check | Verdict |
|---|-------|---------|
| 1 | Heartbeat detection condition | **DIFFERS** |
| 2 | Missed-beat counter / threshold | **DIFFERS** (functionally equivalent) |
| 3 | Timer interval | **MATCHES** |
| 4 | missedHeartbeats reset on heartbeat | **MATCHES** (functionally equivalent) |
| 5 | Disconnection fires only once | **MATCHES** |
| 6 | Reconnection triggered by heartbeat | **DIFFERS** |
| 7 | Recovery API URL patterns | **MATCHES** |
| 8 | Timer restart after AMQP reconnection | **MATCHES** (functionally equivalent) |
| A | Notification codes (100/101) | **MISSING** |
| B | Recovery data in reconnection event | **MISSING** |
| C | `recoverOnReconnect` flag unused | **BUG** (dead code) |

---

## Detailed Findings

### 1. Heartbeat Detection Condition — DIFFERS

**TypeScript (expected):** Heartbeat messages are identified by `message.at && !message.type` — the presence of an `at` field and the **absence** of a `type` field. All other messages have a `type` field.

**Java (actual):** Heartbeats are identified by checking `"v1.beat".equals(type)`.

- `RabbitMQFeed.java:177` — `String type = json.has("type") ? json.get("type").asText() : rk;`
- `RabbitMQFeed.java:181` — `if ("v1.beat".equals(type))`

**Impact:** The Java SDK uses a fundamentally different detection mechanism. If server-sent heartbeat messages follow the TypeScript convention (having `at` but no `type` field), the Java code would fall through to the routing-key fallback at line 177. The heartbeat would only be recognized if the routing key happens to be `"v1.beat"`. Additionally, the Java SDK does **not** verify the presence of the `at` field, so a message with `type: "v1.beat"` but no `at` field would still be treated as a heartbeat.

**Risk:** If the message format changes or varies between environments, heartbeats could be silently missed, leading to spurious disconnection events.

---

### 2. Missed-Beat Counter — DIFFERS (Functionally Equivalent)

**TypeScript (expected):** Uses an explicit `missedHeartbeats` counter. Each 10-second timeout increments the counter by 1. Disconnection triggers when `missedHeartbeats >= 3` (~30 seconds total). The counter provides discrete visibility into degradation.

**Java (actual):** Uses a continuous duration comparison with no counter.

- `EventHandler.java:28` — `HEARTBEAT_INTERVAL = Duration.ofSeconds(10)`
- `EventHandler.java:30` — `MAX_MISSED = HEARTBEAT_INTERVAL.multipliedBy(3)` (= 30 seconds)
- `EventHandler.java:139` — `Duration.between(lastBeat, Instant.now()).compareTo(MAX_MISSED) > 0`

**Impact:** Both achieve the same ~30-second disconnection threshold. The Java approach is slightly more precise (checks exact elapsed time), but lacks the discrete counter that the TypeScript SDK uses. There is no way for the Java SDK to emit intermediate "degradation" signals (e.g., "1 beat missed", "2 beats missed") before full disconnection.

---

### 3. Timer Interval — MATCHES

**TypeScript (expected):** 10-second interval.

**Java (actual):**
- `EventHandler.java:28` — `Duration.ofSeconds(10)`
- `EventHandler.java:62-69` — `scheduleAtFixedRate(..., HEARTBEAT_INTERVAL.toMillis(), HEARTBEAT_INTERVAL.toMillis(), ...)`

Both use a 10-second check interval.

---

### 4. missedHeartbeats Reset — MATCHES (Functionally Equivalent)

**TypeScript (expected):** `missedHeartbeats = 0` on every received heartbeat.

**Java (actual):**
- `EventHandler.java:88` — `lastBeat = Instant.now();`

Resetting `lastBeat` to the current time serves the same purpose as zeroing a counter. The next `checkHeartbeat()` invocation will see that insufficient time has elapsed and will not trigger disconnection. Functionally identical.

---

### 5. Disconnection Fires Only Once — MATCHES

**TypeScript (expected):** If already in disconnected state, the disconnection event must not fire again.

**Java (actual):** Both disconnection paths have guards:
- `EventHandler.java:127` — `handleDisconnection()` checks `if (!disconnected)` before setting state and emitting
- `EventHandler.java:139` — `checkHeartbeat()` checks `if (!disconnected && ...)` before triggering

Once `disconnected` is set to `true`, neither path can fire again until `heartbeat()` resets it to `false`.

---

### 6. Reconnection Triggered by Heartbeat — DIFFERS

**TypeScript (expected):** Recovery starts when a **real heartbeat message** arrives while in disconnected state. The AMQP connection event alone should **not** trigger recovery.

**Java (actual):**
- `EventHandler.java:87-121` — `heartbeat()` checks `if (disconnected)` and runs recovery. This part is correct.
- `RabbitMQFeed.java:95` — `handler.heartbeat()` is called **synthetically** from `connect()` immediately after establishing the AMQP connection.

**Impact:** The Java SDK triggers recovery as soon as the AMQP transport reconnects, before any real heartbeat message has arrived. The `connect()` method at line 93-95 calls `establish()`, `startConsumers()`, then `handler.heartbeat()`. This synthetic heartbeat fires recovery immediately on transport reconnection, which contradicts the TypeScript reference that says recovery should only start when a real heartbeat arrives.

This matters because the AMQP connection may be re-established but the server may not yet be ready to serve data. Triggering recovery too early could result in failed API calls or incomplete data.

---

### 7. Recovery API URL Patterns — MATCHES

**TypeScript (expected):**
```
GET {apiBaseURL}/recover_markets/{disconnectionTimestamp}?token={apiToken}
GET {apiBaseURL}?range[modified_at]={disconnectionTimestamp},{reconnectionTimestamp}&filter[booked]=true&token={apiToken}
```

**Java (actual):**
- `MatchesClient.java:97-100`:
  ```java
  String.format("%s/recover_markets/%s?token=%s",
      opts.getApiBaseUrl(), since, opts.getApiToken())
  ```
- `MatchesClient.java:121-124`:
  ```java
  String.format("%s?range[modified_at]=%s,%s&filter[booked]=true&token=%s",
      opts.getApiBaseUrl(), start, end, opts.getApiToken())
  ```

Both URL patterns are identical. The timestamps are passed as ISO-8601 strings (`Instant.toString()`).

---

### 8. Timer Restart After AMQP Reconnection — MATCHES (Functionally Equivalent)

**TypeScript (expected):** The disconnection timer must be explicitly restarted after AMQP transport-level reconnection.

**Java (actual):** The `EventHandler` uses `scheduleAtFixedRate` (`EventHandler.java:62-70`) which starts in the constructor and runs **continuously** — it is never cancelled or restarted. When AMQP reconnects:

1. `handleDisconnection()` was called on shutdown (sets `disconnected = true`)
2. `connect()` calls `handler.heartbeat()` (resets `lastBeat` to now)
3. The still-running timer sees a recent `lastBeat` and does not trigger disconnection

**Impact:** Functionally equivalent — the Java timer never stops, so "restarting" it is unnecessary. The `lastBeat` reset ensures correct behavior. The only edge case would be if the `EventHandler` were shut down and recreated, but this doesn't happen in the current codebase.

---

## Additional Findings

### A. Notification Codes — MISSING

**TypeScript (expected):** Disconnection emits notification code `100`; reconnection emits code `101`.

**Java (actual):**
- `EventHandler.java:132` — `sink.accept("disconnection")` (string only)
- `EventHandler.java:144` — `sink.accept("disconnection")` (string only)
- `EventHandler.java:118` — `sink.accept("reconnection")` (string only)

The Java SDK emits plain strings through a `Consumer<String>` sink. No numeric codes are used. Customer code receives `"disconnection"` or `"reconnection"` strings with no additional metadata.

---

### B. Recovery Data in Reconnection Event — MISSING

**TypeScript (expected):** The reconnection notification (code 101) includes the recovery data from both API calls. Both calls complete **before** the reconnection notification is emitted.

**Java (actual):**
- `EventHandler.java:100-104` — Recovery API calls are made
- `EventHandler.java:118` — `sink.accept("reconnection")` passes only the string

The ordering is correct (API calls happen before notification), but the **recovery data is discarded** — it is not included in the reconnection event. The return values of `recoverMarkets()` and `fetchMatchesRange()` are never stored or forwarded.

---

### C. `recoverOnReconnect` Config Flag — DEAD CODE

**Defined at:** `SDKOptions.java:75-83`
```java
@Builder.Default
private final boolean recoverOnReconnect = true;
```

**Usage:** Searched the entire `src/main/java` tree — this field is **never read** outside its definition. The recovery logic in `EventHandler.heartbeat()` (lines 100-104) unconditionally calls the recovery APIs without checking this flag.

---

## Files Examined

| File | Lines | Role |
|------|-------|------|
| `src/main/java/com/pandascore/sdk/events/EventHandler.java` | 1-166 | Heartbeat monitoring, disconnection detection, recovery orchestration |
| `src/main/java/com/pandascore/sdk/rmq/RabbitMQFeed.java` | 1-321 | AMQP connection, message consumption, reconnection, message buffering |
| `src/main/java/com/pandascore/sdk/http/MatchesClient.java` | 1-196 | Recovery HTTP API calls |
| `src/main/java/com/pandascore/sdk/config/SDKOptions.java` | 1-126 | Configuration (including unused `recoverOnReconnect`) |

---

## Recommended Fixes (Priority Order)

1. **[High] Heartbeat detection** — Align with the TypeScript convention: check for `at` field presence and `type` field absence, or confirm with the backend team that `v1.beat` is the canonical heartbeat identifier for the Java feed.

2. **[High] Remove synthetic heartbeat from `connect()`** — `RabbitMQFeed.java:95` should not call `handler.heartbeat()`. Recovery should only trigger from a real heartbeat message, not from transport reconnection.

3. **[Medium] Include recovery data in reconnection event** — Change the sink type from `Consumer<String>` to something that can carry the recovery payload alongside the event type (e.g., a typed event object with code + data).

4. **[Medium] Add notification codes** — Emit codes 100 (disconnection) and 101 (reconnection) to match the TypeScript SDK's contract.

5. **[Low] Wire up `recoverOnReconnect`** — Either check the flag in `EventHandler.heartbeat()` before making recovery API calls, or remove it from `SDKOptions` to avoid confusion.

6. **[Low] Add explicit missed-heartbeat counter** — While the duration-based approach works, an explicit counter would enable intermediate degradation signals and make the logic easier to reason about.
