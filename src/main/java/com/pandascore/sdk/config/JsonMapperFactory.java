package com.pandascore.sdk.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared ObjectMapper factory for the SDK.
 * <p>
 * All SDK components should use {@link #create()} to obtain a consistently
 * configured ObjectMapper instance rather than constructing their own.
 */
public final class JsonMapperFactory {

    private JsonMapperFactory() {}

    /**
     * Creates a new ObjectMapper configured for SDK usage:
     * <ul>
     *   <li>Excludes null values from serialization</li>
     *   <li>Supports Java Time types (Instant, etc.)</li>
     * </ul>
     *
     * @return a new, pre-configured ObjectMapper
     */
    public static ObjectMapper create() {
        return new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule());
    }
}
