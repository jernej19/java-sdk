package com.pandascore.sdk.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SDKOptions configuration, focusing on QoS prefetch defaults.
 */
class SDKOptionsTest {

    private SDKOptions.SDKOptionsBuilder minimalBuilder() {
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

    @Test
    @DisplayName("Default prefetchCount is 1")
    void defaultPrefetchCount_is1() {
        SDKOptions opts = minimalBuilder().build();
        assertEquals(1, opts.getPrefetchCount());
    }

    @Test
    @DisplayName("Custom prefetchCount is respected")
    void customPrefetchCount_isRespected() {
        SDKOptions opts = minimalBuilder().prefetchCount(50).build();
        assertEquals(50, opts.getPrefetchCount());
    }

    @Test
    @DisplayName("PrefetchCount of 0 means unlimited (allowed but not recommended)")
    void prefetchCount_zero_isAllowed() {
        SDKOptions opts = minimalBuilder().prefetchCount(0).build();
        assertEquals(0, opts.getPrefetchCount());
    }
}
