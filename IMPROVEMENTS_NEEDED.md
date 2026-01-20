# Java SDK Improvements - Based on TypeScript SDK Comparison

This document outlines the improvements needed for the Java SDK based on the more sophisticated TypeScript SDK implementation.

## Executive Summary

The TypeScript SDK is significantly more complete with:
- **15+ missing data type classes**
- **1 missing HTTP API method**
- **40+ missing fields across existing classes**
- More robust configuration options
- Better event notification system

---

## 1. MISSING DATA TYPE CLASSES

### 1.1 Fixture-Related Types (High Priority)

#### **Player.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Player {
    private Long id;
    private String name;
    private String slug;
    private String role;  // nullable
}
```
**Usage**: Referenced in `FixtureTeam.players` field

---

#### **League.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class League {
    private Long id;
    private String name;
    private String slug;
    @JsonProperty("image_url") private String imageUrl;
    private String url;
    @JsonProperty("modified_at") private String modifiedAt;
}
```
**Usage**: Referenced in `FixtureMatch.league` field

---

#### **Tournament.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tournament {
    private Long id;
    private String name;
    private String slug;
    @JsonProperty("begin_at") private String beginAt;
    @JsonProperty("end_at") private String endAt;
    @JsonProperty("league_id") private Long leagueId;
    @JsonProperty("serie_id") private Long serieId;
    private String tier;
    private String type;  // "online", "offline"
    private String region;
    private String country;
    private String prizepool;
    @JsonProperty("live_supported") private Boolean liveSupported;
    @JsonProperty("winner_id") private Long winnerId;
    @JsonProperty("winner_type") private String winnerType;
    @JsonProperty("modified_at") private String modifiedAt;
}
```
**Usage**: Referenced in `FixtureMatch.tournament` field

---

#### **Videogame.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Videogame {
    private Long id;
    private String name;
    private String slug;
}
```
**Usage**: Referenced in `FixtureMatch.videogame` field

---

#### **VideogameVersion.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideogameVersion {
    private String name;
    private Boolean current;
}
```
**Usage**: Referenced in `FixtureMatch.videogameVersion` field

---

#### **MatchResult.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchResult {
    @JsonProperty("team_id") private Long teamId;
    private Integer score;
}
```
**Usage**: Match-level score results

---

#### **Live.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Live {
    @JsonProperty("opens_at") private String opensAt;
    private Boolean supported;
    private String url;  // WebSocket URL
}
```
**Usage**: Live stream/scoreboard information

---

#### **StreamInfo.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamInfo {
    @JsonProperty("embed_url") private String embedUrl;
    private String language;
    private Boolean main;
    private Boolean official;
    @JsonProperty("raw_url") private String rawUrl;
}
```
**Usage**: Detailed stream information

---

#### **Streams.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Streams {
    // Map<language, StreamData>
    // Dynamic keys like "english", "russian", "official"
    private Map<String, StreamData> streams;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StreamData {
        @JsonProperty("embed_url") private String embedUrl;
        @JsonProperty("raw_url") private String rawUrl;
    }
}
```
**Usage**: Legacy streams object by language

---

#### **GameMap.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameMap {
    private Long id;
    private String name;
    private String slug;
    private String image;
    @JsonProperty("game_mode") private String gameMode;
}
```
**Usage**: Map information for games

---

#### **GameWinner.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameWinner {
    private Long id;
    private String type;  // "Team" | "Player"
}
```
**Usage**: Game-level winner information

---

#### **GameRoundTeam.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameRoundTeam {
    private String outcome;  // "eliminated", "exploded", "defused"
    private Integer round;
    @JsonProperty("team_id") private Long teamId;
    private Boolean terrorist;
    private Boolean winner;
}
```
**Usage**: Detailed round-by-round CS:GO data

---

#### **RoundScore.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoundScore {
    @JsonProperty("team_id") private Long teamId;
    private Integer score;
}
```
**Usage**: Round scores by team

---

#### **Game.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Game {
    private Long id;
    private Integer position;
    @JsonProperty("match_id") private Long matchId;
    private String status;  // GameStatus enum
    @JsonProperty("begin_at") private String beginAt;
    @JsonProperty("end_at") private String endAt;
    private Boolean finished;
    private Boolean complete;
    @JsonProperty("detailed_stats") private Boolean detailedStats;
    private Boolean draw;
    private Boolean forfeit;
    private Long length;  // duration in seconds
    private GameMap map;
    @JsonProperty("number_of_rounds") private Integer numberOfRounds;
    @JsonProperty("rounds_score") private List<RoundScore> roundsScore;
    @JsonProperty("video_url") private String videoUrl;
    private GameWinner winner;
    @JsonProperty("winner_type") private String winnerType;
    @JsonProperty("game_round_teams") private List<GameRoundTeam> gameRoundTeams;  // Optional
}
```
**Usage**: Individual game/map within a match

---

### 1.2 Enums (High Priority)

#### **MatchStatus.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

public enum MatchStatus {
    NOT_BOOKED("not_booked"),
    PENDING("pending"),
    PRE_MATCH("pre_match"),
    LIVE("live"),
    POSTPONED("postponed"),
    FINISHED("finished"),
    SETTLED("settled"),
    CANCELED("canceled");

    private final String value;

    MatchStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
```

---

#### **MatchType.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

public enum MatchType {
    BEST_OF("best_of"),
    OW_BEST_OF("ow_best_of"),
    FIRST_TO("first_to"),
    RED_BULL_HOME_GROUND("red_bull_home_ground"),
    CUSTOM("custom");

    private final String value;

    MatchType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
```

---

#### **GameStatus.java**
```java
package com.pandascore.sdk.model.feed.fixtures;

public enum GameStatus {
    NOT_STARTED("not_started"),
    RUNNING("running"),
    FINISHED("finished"),
    NOT_PLAYED("not_played");

    private final String value;

    GameStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
```

---

### 1.3 Scoreboard Timer Objects (Medium Priority)

#### **EsoccerTimerObject.java**
```java
package com.pandascore.sdk.model.feed.scoreboard;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EsoccerTimerObject {
    private Integer timer;  // 0-600 seconds (counts UP)
    private Boolean paused;
    @JsonProperty("issued_at") private String issuedAt;  // ISO8601
}
```

---

#### **EbasketballTimerObject.java**
```java
package com.pandascore.sdk.model.feed.scoreboard;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EbasketballTimerObject {
    private Integer timer;  // 300→0 seconds (counts DOWN)
    private Boolean paused;
    @JsonProperty("issued_at") private String issuedAt;  // ISO8601
}
```

---

#### **EhockeyTimerObject.java** (NEW GAME SUPPORT)
```java
package com.pandascore.sdk.model.feed.scoreboard;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EhockeyTimerObject {
    private Integer timer;  // 300→0 seconds (counts DOWN)
    private Boolean paused;
    @JsonProperty("issued_at") private String issuedAt;
}
```

---

#### **LolTimerObject.java**
Already exists but needs to be used as an alternative to Integer in `LolGame.timer`

---

### 1.4 NEW Scoreboard Support: eHockey

#### **ScoreboardEhockey.java**
```java
package com.pandascore.sdk.model.feed.scoreboard;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreboardEhockey {
    private Long id;
    @JsonProperty("updated_at") private String updatedAt;
    private List<EhockeyGame> games;
    @JsonProperty("scoreboard_type") private String scoreboardType;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EhockeyGame {
        private Long id;
        private Integer position;
        private String status;
        private EhockeyTimerObject timer;
        @JsonProperty("current_period") private Integer currentPeriod;
        private List<EhockeyPlayer> players;
        private List<EhockeyPeriod> periods;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EhockeyPlayer {
        private Long id;
        @JsonProperty("goal_score") private Integer goalScore;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EhockeyPeriod {
        private Integer index;
        private List<EhockeyPlayer> players;
    }
}
```

---

## 2. MISSING FIELDS IN EXISTING CLASSES

### 2.1 FixtureMatch.java - **28 Missing Fields**

Current Java SDK has only **9 fields**, TypeScript has **37 fields**.

**Add these fields:**

```java
// Basic match info
private String slug;
@JsonProperty("original_scheduled_at") private String originalScheduledAt;
@JsonProperty("end_at") private String endAt;
@JsonProperty("modified_at") private String modifiedAt;

// Tournament/League references
@JsonProperty("serie_id") private Long serieId;
@JsonProperty("tournament_id") private Long tournamentId;
private League league;
private Tournament tournament;
private Videogame videogame;
@JsonProperty("videogame_version") private VideogameVersion videogameVersion;

// Match configuration
@JsonProperty("match_type") private String matchType;  // or MatchType enum
@JsonProperty("number_of_games") private Integer numberOfGames;
@JsonProperty("game_advantage") private Long gameAdvantage;

// Match state flags
private Boolean draw;
private Boolean forfeit;
private Boolean rescheduled;

// Results and games
private List<MatchResult> results;
private List<Game> games;

// Streaming
private Live live;
@JsonProperty("live_embed_url") private String liveEmbedUrl;
@JsonProperty("official_stream_url") private String officialStreamUrl;
private Streams streams;
@JsonProperty("streams_list") private List<StreamInfo> streamsList;

// Winner details (already has winner but should expand)
@JsonProperty("winner_id") private Long winnerId;
```

---

### 2.2 FixtureTeam.java - **3 Missing Fields**

```java
@JsonProperty("dark_mode_image_url") private String darkModeImageUrl;
@JsonProperty("modified_at") private String modifiedAt;
private List<Player> players;
```

---

### 2.3 FixtureWinner.java - **1 Missing Field**

```java
@JsonProperty("dark_mode_image_url") private String darkModeImageUrl;
```

---

### 2.4 MarketsMessageSelection.java - **2 Missing Fields**

```java
@JsonProperty("opponent_type") private String opponentType;  // "Team" | "Player"
@JsonProperty("probability_with_margin") private Double probabilityWithMargin;
```

---

### 2.5 Scoreboard Classes - Timer Field Type Issues

**Problem**: Current implementation uses `Optional<Integer>` or `Integer` for timers, but TypeScript SDK shows some games return timer **objects** with `{timer, paused, issued_at}`.

**ScoreboardEsoccer.EsoccerGame**:
```java
// Current: No timer field at all
// Add:
private EsoccerTimerObject timer;
@JsonProperty("current_half") private Integer currentHalf;
```

**ScoreboardEbasketball.EbasketballGame**:
```java
// Current: No timer field at all
// Add:
private EbasketballTimerObject timer;
@JsonProperty("current_quarter") private Integer currentQuarter;
```

**ScoreboardLol.LolGame**:
```java
// Current: private Optional<LolTimerObject> timer;
// This is correct - keep as is, but document that it can be Integer or LolTimerObject
```

---

## 3. MISSING HTTP API METHODS

### 3.1 MatchesClient.java - Add `fetchMatch()` Method

**TypeScript SDK has**: `fetchMatch(id: string): Promise<Match>`

**Add to Java MatchesClient**:
```java
/**
 * Fetch a single match by ID.
 *
 * @param id Match ID
 * @return Match details
 * @throws IOException on network or parsing errors
 */
public static FixtureMatch fetchMatch(String id) throws IOException {
    SDKOptions opts = SDKConfig.getInstance().getOptions();
    MDC.put("customerId", String.valueOf(opts.getCompanyId()));
    MDC.put("operation", "fetchMatch");
    String url = String.format(
        "%s/%s?token=%s",
        opts.getApiBaseUrl(), id, opts.getApiToken()
    );
    try {
        return get(url, new TypeReference<FixtureMatch>() {});
    } finally {
        MDC.remove("operation");
        MDC.remove("customerId");
    }
}
```

---

### 3.2 MatchesClient.java - Add `fetchMarkets()` Method

**TypeScript SDK has**: `fetchMarkets(matchId: string): Promise<EnrichedMarket[]>`

**Add to Java MatchesClient**:
```java
/**
 * Response wrapper for markets API.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public static class MarketsResponse {
    private List<GameMarkets> games;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameMarkets {
        private List<MarketsMessageMarket> markets;
    }
}

/**
 * Fetch markets for a specific match.
 *
 * @param matchId Match ID
 * @return List of markets across all games
 * @throws IOException on network or parsing errors
 */
public static List<MarketsMessageMarket> fetchMarkets(String matchId) throws IOException {
    SDKOptions opts = SDKConfig.getInstance().getOptions();
    MDC.put("customerId", String.valueOf(opts.getCompanyId()));
    MDC.put("operation", "fetchMarkets");
    String url = String.format(
        "%s/%s/markets?token=%s",
        opts.getApiBaseUrl(), matchId, opts.getApiToken()
    );
    try {
        MarketsResponse response = get(url, new TypeReference<MarketsResponse>() {});
        return response.getGames().stream()
            .flatMap(game -> game.getMarkets().stream())
            .collect(Collectors.toList());
    } finally {
        MDC.remove("operation");
        MDC.remove("customerId");
    }
}
```

---

## 4. CONFIGURATION IMPROVEMENTS

### 4.1 SDKOptions.java - Add Event Notification Flag

**TypeScript SDK has**: `recoverOnReconnect?: boolean` (default: true)

This allows users to disable automatic recovery and handle it manually via events.

**Add to SDKOptions**:
```java
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SDKOptions {
    // ... existing fields ...

    /**
     * Whether to automatically trigger recovery (recoverMarkets + fetchMatchesRange)
     * when reconnection occurs. Default: true.
     *
     * If false, SDK emits reconnection event but does NOT call recovery APIs.
     * User must manually call MatchesClient methods if they want recovery.
     */
    @Builder.Default
    private boolean recoverOnReconnect = true;

    // ... rest of class ...
}
```

**Update EventHandler.java** to check this flag before calling recovery methods.

---

## 5. DOCUMENTATION & TYPE SAFETY IMPROVEMENTS

### 5.1 Create Comprehensive Javadoc for Data Models

TypeScript SDK has excellent inline comments explaining each field. Java SDK should match this.

**Example** (FixtureMatchBettingMetadata):
```java
/**
 * Betting configuration and metadata for a match.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureMatchBettingMetadata {
    /** Whether BetBuilder is enabled for this match */
    @JsonProperty("betbuilder_enabled") private Boolean betbuilderEnabled;

    /** Applied betting market group */
    @JsonProperty("betting_group") private BettingGroup bettingGroup;

    /** Applied market blueprint template */
    private Blueprint blueprint;

    /** Is match available for booking */
    private Boolean bookable;

    /** Has match been booked */
    private Boolean booked;

    /** Timestamp when match was booked (ISO8601) */
    @JsonProperty("booked_at") private String bookedAt;

    /** User ID who booked the match */
    @JsonProperty("booked_by_user_id") private Long bookedByUserId;

    /** Match coverage level */
    private String coverage;

    /** Are inputs enabled for this match */
    @JsonProperty("inputs_enable") private Boolean inputsEnable;

    /** Are live odds available */
    @JsonProperty("live_available") private Boolean liveAvailable;

    /** Have markets been created */
    @JsonProperty("markets_created") private Boolean marketsCreated;

    /** Last time markets were updated (ISO8601) */
    @JsonProperty("markets_updated_at") private String marketsUpdatedAt;

    /** Are micromarkets enabled */
    @JsonProperty("micromarkets_enabled") private Boolean micromarketsEnabled;

    /** Has match been reviewed by PandaScore */
    @JsonProperty("pandascore_reviewed") private Boolean pandascoreReviewed;

    /** Is match fully settled */
    private Boolean settled;
}
```

---

### 5.2 Add README Examples

Create `README.md` with:
- Initialization example
- Feed consumption example
- Recovery API examples
- Event handling examples
- Configuration options table

Similar to TypeScript SDK's excellent documentation.

---

## 6. PRIORITY SUMMARY

### **Critical (Must Have)**
1. ✅ Add missing Fixture data types: `League`, `Tournament`, `Videogame`, `VideogameVersion`, `Player`, `Game`
2. ✅ Add missing fields to `FixtureMatch` (28 fields)
3. ✅ Add HTTP API methods: `fetchMatch()`, `fetchMarkets()`
4. ✅ Add missing enums: `MatchStatus`, `MatchType`, `GameStatus`

### **High Priority**
5. ✅ Add streaming types: `Live`, `StreamInfo`, `Streams`
6. ✅ Add game detail types: `GameMap`, `GameWinner`, `MatchResult`, `RoundScore`
7. ✅ Add timer object types for scoreboards
8. ✅ Fix scoreboard timer fields to support timer objects
9. ✅ Add `recoverOnReconnect` configuration option

### **Medium Priority**
10. ✅ Add eHockey scoreboard support (`ScoreboardEhockey`)
11. ✅ Add missing fields to `FixtureTeam` (3 fields)
12. ✅ Add detailed round data: `GameRoundTeam`
13. ✅ Comprehensive Javadoc documentation

### **Low Priority**
14. ✅ Add README with usage examples
15. ✅ Consider builder pattern for complex object construction
16. ✅ Add validation utilities (similar to TypeScript's bet validation)

---

## 7. ITEMS EXPLICITLY EXCLUDED (Per User Request)

❌ **RTBL (Real-Time Bet Logging)** - TypeScript has `RTBL` class with bet publishing
❌ **Market Limits** - TypeScript has `limitManager.ts` with hierarchical limit calculation

---

## 8. IMPLEMENTATION NOTES

### 8.1 Field Nullability
- Use `@Nullable` annotations or Java `Optional<T>` for fields that can be null
- TypeScript uses `| null` or `?` for optional fields - map these appropriately

### 8.2 Enums vs Strings
- TypeScript uses string literal unions for status/type fields
- Java should use enums with `@JsonValue` for better type safety
- Keep string fields for forward compatibility with unknown values

### 8.3 Nested Types
- TypeScript uses inline interface definitions
- Java should use nested static classes (already done for `FixtureSerie.VideogameTitle`)

### 8.4 Collections
- Always use `List<T>` instead of arrays for consistency
- Initialize collections in constructors if using Lombok `@Builder`

---

## ESTIMATED IMPACT

**Total Work**:
- ~15 new classes
- ~3 new enums
- ~35 field additions to existing classes
- ~2 new API methods
- ~1 configuration option

**Benefits**:
- Feature parity with TypeScript SDK
- Complete fixture/match data model
- Full streaming information support
- Richer scoreboard data with timers
- Better type safety with enums
- Enhanced configurability

**Risk**: Low - All additions are additive (backward compatible)
