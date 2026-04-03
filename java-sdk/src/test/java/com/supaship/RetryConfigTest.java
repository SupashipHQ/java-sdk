package com.supaship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryConfigTest {

    @Test
    void defaults_match_javascript_sdk() {
        RetryConfig r = RetryConfig.defaultRetry();
        assertTrue(r.enabled());
        assertEquals(3, r.maxAttempts());
        assertEquals(1000L, r.backoffMs());
    }

    @Test
    void rejectsInvalidMaxAttempts() {
        assertThrows(IllegalArgumentException.class, () -> new RetryConfig(true, 0, 100));
    }
}
