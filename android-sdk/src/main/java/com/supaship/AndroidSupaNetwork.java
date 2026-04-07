package com.supaship;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Android-side network defaults (mirrors the JVM {@code com.supaship.NetworkConfig}): {@link NetworkSettings} plus
 * the {@link Executor} used to run blocking HTTP I/O (typically {@link java.net.HttpURLConnection}).
 */
public final class AndroidSupaNetwork {

    private final NetworkSettings settings;
    private final Executor executor;

    private AndroidSupaNetwork(NetworkSettings settings, Executor executor) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public @NotNull NetworkSettings settings() {
        return settings;
    }

    public @NotNull Executor executor() {
        return executor;
    }

    /**
     * @throws IllegalArgumentException if {@code config.networkSettings()} does not equal {@link #settings()}
     */
    public @NotNull SupaClient client(@NotNull SupaClientConfig config) {
        Objects.requireNonNull(config, "config");
        if (!config.networkSettings().equals(settings)) {
            throw new IllegalArgumentException(
                    "SupaClientConfig.networkSettings() must match AndroidSupaNetwork.settings(); "
                            + "use SupaClientConfig.builder().networkSettings(network.settings()) when building config.");
        }
        return new SupaClient(config, new AndroidEvaluateTransport(executor));
    }

    public static final class Builder {

        private final NetworkSettings.Builder inner = NetworkSettings.builder();
        private Executor executor = ForkJoinPool.commonPool();

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

        public @NotNull Builder requestTimeoutMs(long requestTimeoutMs) {
            inner.requestTimeoutMs(requestTimeoutMs);
            return this;
        }

        /** Executor for URLConnection I/O; use your app/coroutine dispatcher-backed executor on Android. */
        public @NotNull Builder executor(Executor executor) {
            if (executor != null) {
                this.executor = executor;
            }
            return this;
        }

        public @NotNull AndroidSupaNetwork build() {
            return new AndroidSupaNetwork(inner.build(), executor);
        }
    }
}
