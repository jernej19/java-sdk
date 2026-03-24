package com.pandascore.sdk.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SDKOptions builder defaults, validation logic, and QueueBinding validation.
 */
class SDKOptionsValidationTest {

    private SDKOptions.SDKOptionsBuilder validBuilder() {
        return SDKOptions.builder()
            .apiToken("token")
            .companyId(1)
            .email("e@e.com")
            .password("pass")
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q")
                .routingKey("r")
                .build());
    }

    // ============================================================
    //  Default values
    // ============================================================

    @Test
    @DisplayName("Default feedHost is trading-feed.pandascore.co")
    void defaultFeedHost() {
        SDKOptions opts = validBuilder().build();
        assertEquals("trading-feed.pandascore.co", opts.getFeedHost());
    }

    @Test
    @DisplayName("Default apiBaseUrl is https://api.pandascore.co/betting/matches")
    void defaultApiBaseUrl() {
        SDKOptions opts = validBuilder().build();
        assertEquals("https://api.pandascore.co/betting/matches", opts.getApiBaseUrl());
    }

    @Test
    @DisplayName("Default alwaysLogPayload is false")
    void defaultAlwaysLogPayload() {
        SDKOptions opts = validBuilder().build();
        assertFalse(opts.isAlwaysLogPayload());
    }

    @Test
    @DisplayName("Default americanOdds is false")
    void defaultAmericanOdds() {
        SDKOptions opts = validBuilder().build();
        assertFalse(opts.isAmericanOdds());
    }

    @Test
    @DisplayName("Default fractionalOdds is false")
    void defaultFractionalOdds() {
        SDKOptions opts = validBuilder().build();
        assertFalse(opts.isFractionalOdds());
    }

    @Test
    @DisplayName("Default recoverOnReconnect is true")
    void defaultRecoverOnReconnect() {
        SDKOptions opts = validBuilder().build();
        assertTrue(opts.isRecoverOnReconnect());
    }

    @Test
    @DisplayName("Default prefetchCount is 1")
    void defaultPrefetchCount() {
        SDKOptions opts = validBuilder().build();
        assertEquals(1, opts.getPrefetchCount());
    }

    // ============================================================
    //  Custom values
    // ============================================================

    @Test
    @DisplayName("Custom feedHost is respected")
    void customFeedHost() {
        SDKOptions opts = validBuilder().feedHost("custom-host.com").build();
        assertEquals("custom-host.com", opts.getFeedHost());
    }

    @Test
    @DisplayName("Custom apiBaseUrl is respected")
    void customApiBaseUrl() {
        SDKOptions opts = validBuilder().apiBaseUrl("http://localhost:8080").build();
        assertEquals("http://localhost:8080", opts.getApiBaseUrl());
    }

    @Test
    @DisplayName("americanOdds can be enabled")
    void americanOddsEnabled() {
        SDKOptions opts = validBuilder().americanOdds(true).build();
        assertTrue(opts.isAmericanOdds());
    }

    @Test
    @DisplayName("fractionalOdds can be enabled")
    void fractionalOddsEnabled() {
        SDKOptions opts = validBuilder().fractionalOdds(true).build();
        assertTrue(opts.isFractionalOdds());
    }

    @Test
    @DisplayName("recoverOnReconnect can be disabled")
    void recoverOnReconnectDisabled() {
        SDKOptions opts = validBuilder().recoverOnReconnect(false).build();
        assertFalse(opts.isRecoverOnReconnect());
    }

    @Test
    @DisplayName("Multiple queueBindings are stored correctly")
    void multipleQueueBindings() {
        SDKOptions opts = SDKOptions.builder()
            .apiToken("t")
            .companyId(1)
            .email("e@e.com")
            .password("p")
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q1").routingKey("r1").build())
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q2").routingKey("r2").build())
            .build();
        assertEquals(2, opts.getQueueBindings().size());
        assertEquals("q1", opts.getQueueBindings().get(0).getQueueName());
        assertEquals("q2", opts.getQueueBindings().get(1).getQueueName());
    }

    // ============================================================
    //  SDKOptions.validate()
    // ============================================================

    @Test
    @DisplayName("validate() passes for valid options")
    void validate_validOptions_passes() {
        assertDoesNotThrow(() -> validBuilder().build().validate());
    }

    @Test
    @DisplayName("validate() throws for null apiToken")
    void validate_nullApiToken_throws() {
        SDKOptions opts = SDKOptions.builder()
            .companyId(1)
            .email("e@e.com")
            .password("p")
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q").routingKey("r").build())
            .build();
        assertThrows(NullPointerException.class, opts::validate);
    }

    @Test
    @DisplayName("validate() throws for zero companyId")
    void validate_zeroCompanyId_throws() {
        SDKOptions opts = validBuilder().companyId(0).build();
        assertThrows(IllegalArgumentException.class, opts::validate);
    }

    @Test
    @DisplayName("validate() throws for negative companyId")
    void validate_negativeCompanyId_throws() {
        SDKOptions opts = validBuilder().companyId(-1).build();
        assertThrows(IllegalArgumentException.class, opts::validate);
    }

    @Test
    @DisplayName("validate() throws for empty queueBindings list")
    void validate_emptyQueueBindings_throws() {
        SDKOptions opts = SDKOptions.builder()
            .apiToken("t")
            .companyId(1)
            .email("e@e.com")
            .password("p")
            .build();
        assertThrows(Exception.class, opts::validate);
    }

    // ============================================================
    //  QueueBinding validation
    // ============================================================

    @Test
    @DisplayName("QueueBinding.validate() passes with valid fields")
    void queueBinding_validate_passes() {
        SDKOptions.QueueBinding qb = SDKOptions.QueueBinding.builder()
            .queueName("my-queue")
            .routingKey("#")
            .build();
        assertDoesNotThrow(qb::validate);
    }

    @Test
    @DisplayName("QueueBinding.validate() throws for null queueName")
    void queueBinding_nullQueueName_throws() {
        SDKOptions.QueueBinding qb = SDKOptions.QueueBinding.builder()
            .routingKey("r")
            .build();
        assertThrows(NullPointerException.class, qb::validate);
    }

    @Test
    @DisplayName("QueueBinding.validate() throws for null routingKey")
    void queueBinding_nullRoutingKey_throws() {
        SDKOptions.QueueBinding qb = SDKOptions.QueueBinding.builder()
            .queueName("q")
            .build();
        assertThrows(NullPointerException.class, qb::validate);
    }

    @Test
    @DisplayName("QueueBinding fields are accessible via getters")
    void queueBinding_getters() {
        SDKOptions.QueueBinding qb = SDKOptions.QueueBinding.builder()
            .queueName("test-queue")
            .routingKey("*.markets.#")
            .build();
        assertEquals("test-queue", qb.getQueueName());
        assertEquals("*.markets.#", qb.getRoutingKey());
    }
}
