package com.supaship;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

/**
 * JVM network layer: {@link NetworkSettings} plus a shared {@link HttpClient} for {@link SupashipClient}.
 *
 * <p>Build a {@link SupashipClient} with {@link #client(SupashipClientConfig)} after aligning
 * {@link SupashipClientConfig.Builder#networkSettings(NetworkSettings)} with {@link #settings()}.
 */
public final class NetworkConfig {

    private final NetworkSettings settings;
    private final HttpClient httpClient;

    private NetworkConfig(NetworkSettings settings, HttpClient httpClient) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Default {@link HttpClient} and the given settings (typical when the SDK key config already carries
     * {@link SupashipClientConfig#networkSettings()}).
     */
    public static @NotNull NetworkConfig fromSettings(@NotNull NetworkSettings settings) {
        Objects.requireNonNull(settings, "settings");
        return new NetworkConfig(settings, HttpClient.newHttpClient());
    }

    public @NotNull NetworkSettings settings() {
        return settings;
    }

    public @NotNull HttpClient httpClient() {
        return httpClient;
    }

    /**
     * @throws IllegalArgumentException if {@code config.networkSettings()} does not equal {@link #settings()}
     */
    public @NotNull SupashipClient client(@NotNull SupashipClientConfig config) {
        Objects.requireNonNull(config, "config");
        if (!config.networkSettings().equals(settings)) {
            throw new IllegalArgumentException(
                    "SupashipClientConfig.networkSettings() must match NetworkConfig.settings(); "
                            + "use SupashipClientConfig.builder().networkSettings(network.settings()) when building config.");
        }
        return new SupashipClient(config, new JavaEvaluateTransport(httpClient));
    }

    public static final class Builder {

        private final NetworkSettings.Builder inner = NetworkSettings.builder();
        private HttpClient httpClient;

        public @NotNull Builder featuresApiUrl(String featuresApiUrl) {
            inner.featuresApiUrl(featuresApiUrl);
            return this;
        }

        public @NotNull Builder eventsApiUrl(String eventsApiUrl) {
            inner.eventsApiUrl(eventsApiUrl);
            return this;
        }

        public @NotNull Builder retry(RetryConfig retry) {
            inner.retry(retry);
            return this;
        }

        public @NotNull Builder requestTimeout(Duration requestTimeout) {
            Objects.requireNonNull(requestTimeout, "requestTimeout");
            inner.requestTimeoutMs(requestTimeout.toMillis());
            return this;
        }

        public @NotNull Builder httpClient(@Nullable HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public @NotNull NetworkConfig build() {
            NetworkSettings built = inner.build();
            HttpClient client = httpClient != null ? httpClient : HttpClient.newHttpClient();
            return new NetworkConfig(built, client);
        }
    }
}
