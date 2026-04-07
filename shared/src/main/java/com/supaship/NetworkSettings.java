package com.supaship;

import java.util.Objects;

/**
 * URLs, retry policy, and timeouts for feature evaluation. Transport-specific details (such as
 * {@link java.net.http.HttpClient}) live in platform modules (Java or Android).
 */
public final class NetworkSettings {

    private final String featuresApiUrl;
    private final String eventsApiUrl;
    private final RetryConfig retry;
    private final long requestTimeoutMs;

    private NetworkSettings(Builder b) {
        this.featuresApiUrl = b.featuresApiUrl;
        this.eventsApiUrl = b.eventsApiUrl;
        this.retry = b.retry;
        this.requestTimeoutMs = b.requestTimeoutMs;
    }

    /**
     * Builder with Supaship production URLs and SDK defaults (10&nbsp;s per request).
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public String featuresApiUrl() {
        return featuresApiUrl;
    }

    public String eventsApiUrl() {
        return eventsApiUrl;
    }

    public RetryConfig retry() {
        return retry;
    }

    public long requestTimeoutMs() {
        return requestTimeoutMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkSettings that = (NetworkSettings) o;
        return requestTimeoutMs == that.requestTimeoutMs
                && Objects.equals(featuresApiUrl, that.featuresApiUrl)
                && Objects.equals(eventsApiUrl, that.eventsApiUrl)
                && Objects.equals(retry, that.retry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(featuresApiUrl, eventsApiUrl, retry, requestTimeoutMs);
    }

    /** Fluent builder; defaults match the JavaScript SDK ({@link Constants}). */
    public static final class Builder {

        private String featuresApiUrl = Constants.DEFAULT_FEATURES_URL;
        private String eventsApiUrl = Constants.DEFAULT_EVENTS_URL;
        private RetryConfig retry = RetryConfig.defaultRetry();
        private long requestTimeoutMs = 10_000L;

        public Builder featuresApiUrl(String featuresApiUrl) {
            this.featuresApiUrl = Objects.requireNonNull(featuresApiUrl, "featuresApiUrl");
            return this;
        }

        public Builder eventsApiUrl(String eventsApiUrl) {
            this.eventsApiUrl = Objects.requireNonNull(eventsApiUrl, "eventsApiUrl");
            return this;
        }

        public Builder retry(RetryConfig retry) {
            this.retry = Objects.requireNonNull(retry, "retry");
            return this;
        }

        public Builder requestTimeoutMs(long requestTimeoutMs) {
            if (requestTimeoutMs < 0) {
                throw new IllegalArgumentException("requestTimeoutMs must be non-negative");
            }
            this.requestTimeoutMs = requestTimeoutMs;
            return this;
        }

        public NetworkSettings build() {
            return new NetworkSettings(this);
        }
    }
}
