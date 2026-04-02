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

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public HttpClient httpClient() {
        return httpClient;
    }

    public static final class Builder {

        private String featuresApiUrl = Constants.DEFAULT_FEATURES_URL;
        private String eventsApiUrl = Constants.DEFAULT_EVENTS_URL;
        private RetryConfig retry = RetryConfig.defaultRetry();
        private Duration requestTimeout = Duration.ofMillis(10_000);
        private HttpClient httpClient;

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

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
            return this;
        }

        /**
         * Optional custom {@link HttpClient} (SSL, proxy, version). When omitted, a default client is created.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public NetworkConfig build() {
            return new NetworkConfig(this);
        }
    }
}
