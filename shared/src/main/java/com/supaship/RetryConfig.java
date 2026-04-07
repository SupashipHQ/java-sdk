package com.supaship;

import java.util.Objects;

/** Retry behavior for feature API requests (exponential backoff, same defaults as the JS SDK). */
public final class RetryConfig {

    private final boolean enabled;
    private final int maxAttempts;
    private final long backoffMs;

    /**
     * Defines how many times {@link SupashipClient} may repeat a failed evaluate call and how long to wait between tries.
     *
     * @param enabled      whether retries run after failures
     * @param maxAttempts  total attempts including the first try; must be ≥ 1
     * @param backoffMs    base delay in milliseconds for exponential backoff (doubling each retry); must be ≥ 0
     * @throws IllegalArgumentException if {@code maxAttempts} or {@code backoffMs} is out of range
     */
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

    /**
     * JavaScript SDK defaults: enabled, 3 attempts, 1000&nbsp;ms base backoff.
     *
     * @return shared-equivalent retry configuration
     */
    public static RetryConfig defaultRetry() {
        return new RetryConfig(true, 3, 1000L);
    }

    /**
     * Whether failed feature requests are retried according to {@link #maxAttempts()} and {@link #backoffMs()}.
     *
     * @return {@code true} if retries are enabled
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Total tries for one logical evaluation request, including the first call.
     *
     * @return maximum attempts (at least 1)
     */
    public int maxAttempts() {
        return maxAttempts;
    }

    /**
     * Base delay before the first retry; later waits multiply this value by powers of two.
     *
     * @return base backoff in milliseconds (non-negative)
     */
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
