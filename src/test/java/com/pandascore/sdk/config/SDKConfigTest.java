package com.pandascore.sdk.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SDKConfig singleton — initialization, retrieval, and validation.
 */
class SDKConfigTest {

    private SDKOptions.SDKOptionsBuilder validBuilder() {
        return SDKOptions.builder()
            .apiToken("token")
            .companyId(1)
            .email("user@example.com")
            .password("secret")
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q1")
                .routingKey("#")
                .build());
    }

    // ============================================================
    //  Successful initialization
    // ============================================================

    @Test
    @DisplayName("setOptions followed by getInstance returns the stored options")
    void setOptions_thenGetInstance_returnsOptions() {
        SDKOptions opts = validBuilder().build();
        SDKConfig.setOptions(opts);
        SDKConfig config = SDKConfig.getInstance();
        assertNotNull(config);
        assertSame(opts, config.getOptions());
    }

    @Test
    @DisplayName("setOptions can be called multiple times — last one wins")
    void setOptions_calledTwice_lastOneWins() {
        SDKOptions first = validBuilder().apiToken("first").build();
        SDKOptions second = validBuilder().apiToken("second").build();
        SDKConfig.setOptions(first);
        SDKConfig.setOptions(second);
        assertEquals("second", SDKConfig.getInstance().getOptions().getApiToken());
    }

    // ============================================================
    //  Null and invalid argument checks
    // ============================================================

    @Test
    @DisplayName("setOptions with null throws NullPointerException")
    void setOptions_null_throws() {
        assertThrows(NullPointerException.class, () -> SDKConfig.setOptions(null));
    }

    @Test
    @DisplayName("setOptions with null apiToken throws NullPointerException")
    void setOptions_nullApiToken_throws() {
        SDKOptions opts = SDKOptions.builder()
            .companyId(1)
            .email("e@e.com")
            .password("p")
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q").routingKey("r").build())
            .build();
        assertThrows(NullPointerException.class, () -> SDKConfig.setOptions(opts));
    }

    @Test
    @DisplayName("setOptions with companyId=0 throws IllegalArgumentException")
    void setOptions_zeroCompanyId_throws() {
        SDKOptions opts = validBuilder().companyId(0).build();
        assertThrows(IllegalArgumentException.class, () -> SDKConfig.setOptions(opts));
    }

    @Test
    @DisplayName("setOptions with negative companyId throws IllegalArgumentException")
    void setOptions_negativeCompanyId_throws() {
        SDKOptions opts = validBuilder().companyId(-5).build();
        assertThrows(IllegalArgumentException.class, () -> SDKConfig.setOptions(opts));
    }

    @Test
    @DisplayName("setOptions with null email throws NullPointerException")
    void setOptions_nullEmail_throws() {
        SDKOptions opts = SDKOptions.builder()
            .apiToken("t")
            .companyId(1)
            .password("p")
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q").routingKey("r").build())
            .build();
        assertThrows(NullPointerException.class, () -> SDKConfig.setOptions(opts));
    }

    @Test
    @DisplayName("setOptions with null password throws NullPointerException")
    void setOptions_nullPassword_throws() {
        SDKOptions opts = SDKOptions.builder()
            .apiToken("t")
            .companyId(1)
            .email("e@e.com")
            .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q").routingKey("r").build())
            .build();
        assertThrows(NullPointerException.class, () -> SDKConfig.setOptions(opts));
    }

    @Test
    @DisplayName("setOptions with empty queueBindings throws NullPointerException")
    void setOptions_emptyQueueBindings_throws() {
        SDKOptions opts = SDKOptions.builder()
            .apiToken("t")
            .companyId(1)
            .email("e@e.com")
            .password("p")
            .build();
        assertThrows(NullPointerException.class, () -> SDKConfig.setOptions(opts));
    }

    @Test
    @DisplayName("setOptions with too many queueBindings throws IllegalArgumentException")
    void setOptions_tooManyQueueBindings_throws() {
        SDKOptions.SDKOptionsBuilder builder = SDKOptions.builder()
            .apiToken("t").companyId(1).email("e@e.com").password("p");
        for (int i = 0; i < SDKOptions.MAX_QUEUES_PER_CONNECTION + 1; i++) {
            builder.queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("q" + i).routingKey("r" + i).build());
        }
        SDKOptions opts = builder.build();
        assertThrows(IllegalArgumentException.class, () -> SDKConfig.setOptions(opts));
    }

    // ============================================================
    //  getInstance before setOptions
    // ============================================================

    @Test
    @DisplayName("getInstance before setOptions throws IllegalStateException")
    void getInstance_beforeSetOptions_throws() throws Exception {
        // Use reflection to clear the singleton for this test
        java.lang.reflect.Field f = SDKConfig.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);

        assertThrows(IllegalStateException.class, SDKConfig::getInstance);

        // Restore valid state for other tests
        SDKConfig.setOptions(validBuilder().build());
    }
}
