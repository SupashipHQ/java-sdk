package com.supaship;

import java.util.Objects;

/** Retry behavior for feature API requests (exponential backoff, same defaults as the JS SDK). */
public final class RetryConfig {

    private final boolean enabled;
    private final int maxAttempts;
    private final long backoffMs;

    public RetryConfig(boolean enabled, int maxAttempts, long backoffMs) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (backoffMs < 0) {
            throw new IllegalArgumentException("backoffMs must be non-negative");
        }
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
    }

    /** JS defaults: enabled true, 3 attempts, 1000 ms base backoff. */
    public static RetryConfig defaultRetry() {
        return new RetryConfig(true, 3, 1000L);
    }

    public boolean enabled() {
        return enabled;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public long backoffMs() {
        return backoffMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RetryConfig that = (RetryConfig) o;
        return enabled == that.enabled
                && maxAttempts == that.maxAttempts
                && backoffMs == that.backoffMs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, maxAttempts, backoffMs);
    }
}
