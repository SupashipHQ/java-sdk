package com.supaship;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

/**
 * JVM network layer: {@link NetworkSettings} plus a shared {@link HttpClient} for {@link SupashipClient}.
 *
 * <p>Prefer {@link SupashipClient#create(SupashipClientConfig)} for the default client, or
 * {@link SupashipClient#create(SupashipClientConfig, SupashipNetwork)} / {@link #client(SupashipClientConfig)}
 * after aligning {@link SupashipClientConfig.Builder#networkSettings(NetworkSettings)} with {@link #settings()}.
 */
public final class NetworkConfig implements SupashipNetwork {

    static {
        SupashipClient.registerDefaultFactory(
                c -> NetworkConfig.fromSettings(c.networkSettings()).client(c));
    }

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
    @Override
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
        private ProxySelector proxySelector;

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

        /**
         * HTTP(S) proxy for {@link HttpClient} (for example {@code http://proxy.corp.com:8080}). Ignored when
         * {@link #httpClient(HttpClient)} is set.
         */
        public @NotNull Builder proxy(@Nullable String proxyUri) {
            if (proxyUri == null || proxyUri.isBlank()) {
                this.proxySelector = null;
                return this;
            }
            URI u = URI.create(proxyUri);
            String host = u.getHost();
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("proxy URI must include a host");
            }
            int port = u.getPort();
            if (port < 0) {
                if ("https".equalsIgnoreCase(u.getScheme())) {
                    port = 443;
                } else {
                    port = 80;
                }
            }
            this.proxySelector = ProxySelector.of(InetSocketAddress.createUnresolved(host, port));
            return this;
        }

        /**
         * Total HTTP attempts per evaluate call (including the first try). Uses the default SDK backoff between tries.
         */
        public @NotNull Builder retries(int maxAttempts) {
            RetryConfig base = RetryConfig.defaultRetry();
            inner.retry(new RetryConfig(true, maxAttempts, base.backoffMs()));
            return this;
        }

        public @NotNull Builder httpClient(@Nullable HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public @NotNull NetworkConfig build() {
            if (httpClient != null && proxySelector != null) {
                throw new IllegalStateException("Set either httpClient or proxy, not both");
            }
            NetworkSettings built = inner.build();
            HttpClient client =
                    httpClient != null
                            ? httpClient
                            : (proxySelector != null
                                    ? HttpClient.newBuilder().proxy(proxySelector).build()
                                    : HttpClient.newHttpClient());
            return new NetworkConfig(built, client);
        }
    }
}
