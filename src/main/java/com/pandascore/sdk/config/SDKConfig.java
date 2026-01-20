package com.pandascore.sdk.config;

import java.util.Objects;

/**
 * Singleton holder for SDKOptions.
 * <p>
 * Must call {@link #setOptions(SDKOptions)} before any other SDK APIs.
 */
public class SDKConfig {
    private static SDKConfig instance;
    private final SDKOptions options;

    private SDKConfig(SDKOptions options) {
        this.options = options;
    }

    /**
     * Initialize the SDK global configuration.
     * <p>
     * Performs null checks on required options and stores a singleton instance.
     *
     * @param options the fully populated SDKOptions
     * @throws NullPointerException if options or any required field is null or queueBindings is empty
     */
    public static void setOptions(SDKOptions options) {
        Objects.requireNonNull(options, "SDKOptions must not be null");
        Objects.requireNonNull(options.getApiToken(), "apiToken must not be null");
        if (options.getCompanyId() <= 0) {
            throw new IllegalArgumentException("companyId must be positive");
        }
        Objects.requireNonNull(options.getEmail(), "email must not be null");
        Objects.requireNonNull(options.getPassword(), "password must not be null");
        Objects.requireNonNull(options.getFeedHost(), "feedHost must not be null");
        Objects.requireNonNull(options.getApiBaseUrl(), "apiBaseUrl must not be null");
        Objects.requireNonNull(options.getQueueBindings(), "queueBindings must not be null");
        if (options.getQueueBindings().isEmpty()) {
            throw new NullPointerException("queueBindings must not be empty");
        }
        instance = new SDKConfig(options);
    }

    /**
     * Retrieve the initialized SDK configuration.
     *
     * @return singleton SDKConfig
     * @throws IllegalStateException if setOptions(...) was not called
     */
    public static SDKConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SDKConfig is not initialized. Call setOptions() first.");
        }
        return instance;
    }

    /**
     * Access the underlying SDKOptions.
     *
     * @return the SDKOptions provided at initialization
     */
    public SDKOptions getOptions() {
        return options;
    }
}
