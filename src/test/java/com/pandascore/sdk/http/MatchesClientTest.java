package com.pandascore.sdk.http;

import com.pandascore.sdk.config.SDKConfig;
import com.pandascore.sdk.config.SDKOptions;
import com.pandascore.sdk.model.feed.fixtures.FixtureMatch;
import com.pandascore.sdk.model.feed.markets.MarketsMessageMarket;
import com.pandascore.sdk.model.feed.markets.MarketsRecoveryMatch;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MatchesClient HTTP methods using MockWebServer.
 */
class MatchesClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        String baseUrl = server.url("/betting/matches").toString();
        // Remove trailing slash if present
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        SDKConfig.setOptions(SDKOptions.builder()
            .apiToken("test-api-token")
            .companyId(42)
            .email("test@test.com")
            .password("pass")
            .apiBaseUrl(baseUrl)
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q").routingKey("r").build())
            .build());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // ============================================================
    //  recoverMarkets
    // ============================================================

    @Test
    @DisplayName("recoverMarkets sends correct URL and deserializes response")
    void recoverMarkets_sendsCorrectUrl() throws Exception {
        String json = "[{\"id\": 100, \"markets\": [], \"games\": []}]";
        server.enqueue(new MockResponse()
            .setBody(json)
            .addHeader("Content-Type", "application/json"));

        List<MarketsRecoveryMatch> result = MatchesClient.recoverMarkets("2025-01-01T00:00:00Z");

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());

        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("/recover_markets/2025-01-01T00:00:00Z"));
        assertTrue(req.getPath().contains("token=test-api-token"));
        assertEquals("GET", req.getMethod());
    }

    @Test
    @DisplayName("recoverMarkets with empty array returns empty list")
    void recoverMarkets_emptyResponse() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("[]")
            .addHeader("Content-Type", "application/json"));

        List<MarketsRecoveryMatch> result = MatchesClient.recoverMarkets("2025-01-01T00:00:00Z");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("recoverMarkets throws IOException on HTTP error")
    void recoverMarkets_httpError_throws() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThrows(IOException.class,
            () -> MatchesClient.recoverMarkets("2025-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("recoverMarkets with multiple matches and games")
    void recoverMarkets_multipleMatches() throws Exception {
        String json = """
            [
              {"id": 1, "markets": [{"id": "m1"}], "games": [{"id": 10, "position": 1, "markets": []}]},
              {"id": 2, "markets": [], "games": []}
            ]
            """;
        server.enqueue(new MockResponse()
            .setBody(json)
            .addHeader("Content-Type", "application/json"));

        List<MarketsRecoveryMatch> result = MatchesClient.recoverMarkets("2025-01-01T00:00:00Z");
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(1, result.get(0).getMarkets().size());
        assertEquals(1, result.get(0).getGames().size());
        assertEquals(2L, result.get(1).getId());
    }

    // ============================================================
    //  fetchMatchesRange
    // ============================================================

    @Test
    @DisplayName("fetchMatchesRange sends correct URL with range params")
    void fetchMatchesRange_sendsCorrectUrl() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("[{\"id\": 200, \"name\": \"T1 vs G2\"}]")
            .addHeader("Content-Type", "application/json"));

        List<FixtureMatch> result = MatchesClient.fetchMatchesRange(
            "2025-01-01T00:00:00Z", "2025-01-02T00:00:00Z");

        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).getId());
        assertEquals("T1 vs G2", result.get(0).getName());

        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("range[modified_at]=2025-01-01T00:00:00Z,2025-01-02T00:00:00Z"));
        assertTrue(req.getPath().contains("filter[booked]=true"));
        assertTrue(req.getPath().contains("token=test-api-token"));
    }

    @Test
    @DisplayName("fetchMatchesRange with empty response returns empty list")
    void fetchMatchesRange_emptyResponse() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("[]")
            .addHeader("Content-Type", "application/json"));

        List<FixtureMatch> result = MatchesClient.fetchMatchesRange("2025-01-01T00:00:00Z", "2025-01-02T00:00:00Z");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("fetchMatchesRange throws IOException on HTTP error")
    void fetchMatchesRange_httpError_throws() {
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThrows(IOException.class,
            () -> MatchesClient.fetchMatchesRange("2025-01-01T00:00:00Z", "2025-01-02T00:00:00Z"));
    }

    // ============================================================
    //  fetchMatch
    // ============================================================

    @Test
    @DisplayName("fetchMatch sends correct URL and deserializes single match")
    void fetchMatch_sendsCorrectUrl() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("{\"id\": 300, \"name\": \"Final\", \"status\": \"finished\"}")
            .addHeader("Content-Type", "application/json"));

        FixtureMatch result = MatchesClient.fetchMatch("300");

        assertEquals(300L, result.getId());
        assertEquals("Final", result.getName());
        assertEquals("finished", result.getStatus());

        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("/300"));
        assertTrue(req.getPath().contains("token=test-api-token"));
    }

    @Test
    @DisplayName("fetchMatch throws IOException on HTTP error")
    void fetchMatch_httpError_throws() {
        server.enqueue(new MockResponse().setResponseCode(403));

        assertThrows(IOException.class, () -> MatchesClient.fetchMatch("999"));
    }

    // ============================================================
    //  fetchMarkets
    // ============================================================

    @Test
    @DisplayName("fetchMarkets flattens markets from all games")
    void fetchMarkets_flattensFromGames() throws Exception {
        String json = """
            {
              "games": [
                {"markets": [{"id": "m1"}, {"id": "m2"}]},
                {"markets": [{"id": "m3"}]}
              ]
            }
            """;
        server.enqueue(new MockResponse()
            .setBody(json)
            .addHeader("Content-Type", "application/json"));

        List<MarketsMessageMarket> result = MatchesClient.fetchMarkets("500");

        assertEquals(3, result.size());
        assertEquals("m1", result.get(0).getId());
        assertEquals("m2", result.get(1).getId());
        assertEquals("m3", result.get(2).getId());

        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("/500/markets"));
        assertTrue(req.getPath().contains("token=test-api-token"));
    }

    @Test
    @DisplayName("fetchMarkets with empty games returns empty list")
    void fetchMarkets_emptyGames() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("{\"games\": []}")
            .addHeader("Content-Type", "application/json"));

        List<MarketsMessageMarket> result = MatchesClient.fetchMarkets("500");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("fetchMarkets throws IOException on HTTP error")
    void fetchMarkets_httpError_throws() {
        server.enqueue(new MockResponse().setResponseCode(502));

        assertThrows(IOException.class, () -> MatchesClient.fetchMarkets("500"));
    }
}
