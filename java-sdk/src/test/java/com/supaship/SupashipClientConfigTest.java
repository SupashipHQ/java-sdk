package com.supaship;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SupashipClientConfigTest {

    @Test
    void builder_requires_sdkKey_and_environment() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        SupashipClientConfig.builder()
                                .environment("prod")
                                .features(Map.of("a", true))
                                .build());
        assertThrows(
                IllegalStateException.class,
                () ->
                        SupashipClientConfig.builder()
                                .sdkKey("k")
                                .features(Map.of("a", true))
                                .build());
    }
}
