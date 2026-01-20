package com.pandascore.sdk.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.model.feed.fixtures.FixtureMatch;
import com.pandascore.sdk.model.feed.markets.MarketsMessageMarket;
import com.pandascore.sdk.model.feed.markets.MarketsRecoveryMatch;
import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HTTP client for PandaScore recovery and match-fetch endpoints.
 * <p>
 * Provides typed methods that return lists of domain objects,
 * with built-in logging, timing, and error handling.
 * All methods assume {@link SDKConfig#setOptions} has been called.
 */
public final class MatchesClient {
    private static final Logger logger = LoggerFactory.getLogger(MatchesClient.class);
    private static final ObjectMapper mapper = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(new JavaTimeModule());
    private static final OkHttpClient HTTP = new OkHttpClient();

    // Prevent instantiation
    private MatchesClient() {}

    /**
     * Executes an HTTP GET request, logs duration and result count,
     * and deserializes JSON to the specified type.
     *
     * @param url the full URL to request
     * @param ref Jackson TypeReference for deserialization
     * @param <T> the return type
     * @return deserialized response body
     * @throws IOException on network or parsing errors
     */
    private static <T> T get(String url, TypeReference<T> ref) throws IOException {
        String requestId = UUID.randomUUID().toString();
        Instant start = Instant.now();
        logger.info("HTTP GET {} (requestId={})", url, requestId);
        try (Response res = HTTP.newCall(new Request.Builder().url(url).get().build()).execute()) {
            if (!res.isSuccessful()) {
                logger.error("HTTP GET {} failed with status {}", url, res.code());
                throw new IOException("HTTP " + res.code());
            }
            T body = mapper.readValue(res.body().byteStream(), ref);
            long tookMs = Duration.between(start, Instant.now()).toMillis();
            int count = body instanceof List<?> ? ((List<?>) body).size() : -1;
            logger.info("HTTP GET {} returned {} records in {}ms", url, count, tookMs);
            try {
                String json = mapper.writeValueAsString(body);
                logger.debug("HTTP Response payload for {}: {}", url, json);
            } catch (Exception e) {
                logger.debug("HTTP Response payload for {}: [unserializable]", url);
            }
            return body;
        } catch (IOException e) {
            long tookMs = Duration.between(start, Instant.now()).toMillis();
            logger.error("HTTP GET {} error after {}ms: {}", url, tookMs, e.toString());
            throw e;
        }
    }

    /**
     * Recover all markets modified since the given timestamp.
     *
     * @param since ISO-8601 timestamp (e.g. 2025-05-22T14:00:00Z)
     * @return list of matches each containing markets and games
     * @throws IOException on network or parsing errors
     */
    public static List<MarketsRecoveryMatch> recoverMarkets(String since) throws IOException {
        SDKOptions opts = SDKConfig.getInstance().getOptions();
        MDC.put("customerId", String.valueOf(opts.getCompanyId()));
        MDC.put("operation", "recoverMarkets");
        String url = String.format(
            "%s/recover_markets/%s?token=%s",
            opts.getApiBaseUrl(), since, opts.getApiToken()
        );
        try {
            return get(url, new TypeReference<List<MarketsRecoveryMatch>>() {});
        } finally {
            MDC.remove("operation");
            MDC.remove("customerId");
        }
    }

    /**
     * Fetch all matches modified in the given time window.
     *
     * @param start ISO-8601 timestamp start
     * @param end ISO-8601 timestamp end
     * @return list of {@link FixtureMatch}
     * @throws IOException on network or parsing errors
     */
    public static List<FixtureMatch> fetchMatchesRange(String start, String end) throws IOException {
        SDKOptions opts = SDKConfig.getInstance().getOptions();
        MDC.put("customerId", String.valueOf(opts.getCompanyId()));
        MDC.put("operation", "fetchMatchesRange");
        String url = String.format(
            "%s?range[modified_at]=%s,%s&filter[booked]=true&token=%s",
            opts.getApiBaseUrl(), start, end, opts.getApiToken()
        );
        try {
            return get(url, new TypeReference<List<FixtureMatch>>() {});
        } finally {
            MDC.remove("operation");
            MDC.remove("customerId");
        }
    }

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
}
