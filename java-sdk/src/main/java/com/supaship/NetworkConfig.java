package com.supaship;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

/**
 * Network settings for the Supaship client. Uses {@link java.net.http.HttpClient} (Java 11+).
 */
public final class NetworkConfig {

    private final String featuresApiUrl;
    private final String eventsApiUrl;
    private final RetryConfig retry;
    private final Duration requestTimeout;
    private final HttpClient httpClient;

    private NetworkConfig(Builder b) {
        this.featuresApiUrl = b.featuresApiUrl;
        this.eventsApiUrl = b.eventsApiUrl;
        this.retry = b.retry;
        this.requestTimeout = b.requestTimeout;
        this.httpClient = b.httpClient != null ? b.httpClient : HttpClient.newBuilder().build();
    }

    /**
     * Starts a builder with Supaship production URLs and SDK defaults.
     *
     * @return builder with Supaship edge URLs, default retry policy, 10s per-request timeout, and a default {@link HttpClient}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * URL used for batched feature evaluation HTTP POSTs.
     *
     * @return base URL for the features evaluate API
     */
    public String featuresApiUrl() {
        return featuresApiUrl;
    }

    /**
     * URL reserved for analytics or events traffic (not used by core flag evaluation today).
     *
     * @return base URL for the events API (reserved for future client features)
     */
    public String eventsApiUrl() {
        return eventsApiUrl;
    }

    /**
     * Policy governing retries when the evaluate request fails transiently.
     *
     * @return retry policy applied to feature evaluation HTTP calls
     */
    public RetryConfig retry() {
        return retry;
    }

    /**
     * Upper bound for blocking on request/response I/O for a single attempt.
     *
     * @return timeout applied to each HTTP request body send and response wait
     */
    public Duration requestTimeout() {
        return requestTimeout;
    }

    /**
     * Shared client for all outbound calls from {@link SupaClient}.
     *
     * @return client used for async requests; never null
     */
    public HttpClient httpClient() {
        return httpClient;
    }

    /** Fluent builder; defaults match the JavaScript SDK ({@link Constants}). */
    public static final class Builder {

        /** Initializes URLs, retry, and timeout to Supaship defaults. */
        public Builder() {}

        private String featuresApiUrl = Constants.DEFAULT_FEATURES_URL;
        private String eventsApiUrl = Constants.DEFAULT_EVENTS_URL;
        private RetryConfig retry = RetryConfig.defaultRetry();
        private Duration requestTimeout = Duration.ofMillis(10_000);
        private HttpClient httpClient;

        /**
         * Overrides the default {@link Constants#DEFAULT_FEATURES_URL}.
         *
         * @param featuresApiUrl non-null evaluate API base URL
         * @return this builder
         */
        public Builder featuresApiUrl(String featuresApiUrl) {
            this.featuresApiUrl = Objects.requireNonNull(featuresApiUrl, "featuresApiUrl");
            return this;
        }

        /**
         * Overrides the default {@link Constants#DEFAULT_EVENTS_URL}.
         *
         * @param eventsApiUrl non-null events API base URL
         * @return this builder
         */
        public Builder eventsApiUrl(String eventsApiUrl) {
            this.eventsApiUrl = Objects.requireNonNull(eventsApiUrl, "eventsApiUrl");
            return this;
        }

        /**
         * Sets how failed evaluate requests are retried.
         *
         * @param retry non-null retry policy for failed feature requests
         * @return this builder
         */
        public Builder retry(RetryConfig retry) {
            this.retry = Objects.requireNonNull(retry, "retry");
            return this;
        }

        /**
         * Sets the per-request timeout passed to {@link java.net.http.HttpRequest.Builder#timeout(Duration)}.
         *
         * @param requestTimeout non-null timeout per HTTP request
         * @return this builder
         */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
            return this;
        }

        /**
         * Optional custom {@link HttpClient} (SSL, proxy, HTTP version). When omitted, a default client is created.
         *
         * @param httpClient client instance, or {@code null} to use the default
         * @return this builder
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Builds an immutable snapshot of network-related settings.
         *
         * @return immutable network configuration
         */
        public NetworkConfig build() {
            return new NetworkConfig(this);
        }
    }
}
