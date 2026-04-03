package com.supaship;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Configuration for {@link SupaClient}. Immutable after {@link Builder#build()}. */
public final class SupaClientConfig {

    private final String sdkKey;
    private final String environment;
    private final Map<String, Object> features;
    private final Map<String, Object> context;
    private final Set<String> sensitiveContextProperties;
    private final NetworkConfig networkConfig;
    private final List<SupaClientListener> listeners;

    private SupaClientConfig(Builder b) {
        this.sdkKey = b.sdkKey;
        this.environment = b.environment;
        this.features = Collections.unmodifiableMap(new HashMap<>(b.features));
        this.context =
                b.context == null
                        ? Collections.emptyMap()
                        : Collections.unmodifiableMap(new HashMap<>(b.context));
        this.sensitiveContextProperties =
                Collections.unmodifiableSet(new HashSet<>(b.sensitiveContextProperties));
        this.networkConfig = b.networkConfig != null ? b.networkConfig : NetworkConfig.builder().build();
        this.listeners = Collections.unmodifiableList(new ArrayList<>(b.listeners));
    }

    /**
     * Begins a new configuration builder.
     *
     * @return new mutable builder; call {@link Builder#build()} to obtain an immutable config
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Credential passed as {@code Authorization: Bearer} to Supaship APIs.
     *
     * @return Supaship SDK key (Bearer token for the API)
     */
    @NotNull
    public String sdkKey() {
        return sdkKey;
    }

    /**
     * Logical target environment for flag evaluation.
     *
     * @return environment name sent to the evaluate API (e.g. production, staging)
     */
    @NotNull
    public String environment() {
        return environment;
    }

    /**
     * Fallback values keyed by feature name (same role as {@code features} in the JS SDK).
     *
     * @return unmodifiable map of feature name to local default when the network or API cannot be used
     */
    @NotNull
    public Map<String, Object> features() {
        return features;
    }

    /**
     * Default evaluation context merged into each request unless overridden per call.
     *
     * @return unmodifiable map; may be empty
     */
    @NotNull
    public Map<String, Object> context() {
        return context;
    }

    /**
     * Property names whose values are replaced with a SHA-256 hex digest before sending to the API.
     *
     * @return unmodifiable set of context keys to hash
     */
    @NotNull
    public Set<String> sensitiveContextProperties() {
        return sensitiveContextProperties;
    }

    /**
     * Transport layer settings for feature evaluation calls.
     *
     * @return endpoints, timeouts, retry policy, and HTTP client used by {@link SupaClient}
     */
    @NotNull
    public NetworkConfig networkConfig() {
        return networkConfig;
    }

    /**
     * Extension hooks registered for this client.
     *
     * @return unmodifiable list of hooks invoked around requests and fallbacks
     */
    @NotNull
    public List<SupaClientListener> listeners() {
        return listeners;
    }

    /** Fluent builder for {@link SupaClientConfig}; {@link #build()} validates required fields. */
    public static final class Builder {

        /** Starts with empty features, listeners, and no default context until set. */
        public Builder() {}

        private String sdkKey;
        private String environment;
        private Map<String, Object> features = new HashMap<>();
        private Map<String, Object> context;
        private Set<String> sensitiveContextProperties = new HashSet<>();
        private NetworkConfig networkConfig;
        private final List<SupaClientListener> listeners = new ArrayList<>();

        /**
         * Sets the Supaship SDK key.
         *
         * @param sdkKey Supaship SDK key; required and must not be blank at {@link #build()}
         * @return this builder
         */
        @NotNull
        public Builder sdkKey(@Nullable String sdkKey) {
            this.sdkKey = sdkKey;
            return this;
        }

        /**
         * Sets the environment name included in evaluate requests.
         *
         * @param environment environment name sent on evaluate requests; required at {@link #build()}
         * @return this builder
         */
        @NotNull
        public Builder environment(@Nullable String environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Replaces feature fallbacks with the given map (null clears to empty).
         *
         * @param features map of feature name to local default value
         * @return this builder
         */
        @NotNull
        public Builder features(@Nullable Map<String, ?> features) {
            this.features.clear();
            if (features != null) {
                for (Map.Entry<String, ?> e : features.entrySet()) {
                    this.features.put(e.getKey(), e.getValue());
                }
            }
            return this;
        }

        /**
         * Sets the default evaluation context ({@code null} means no default context).
         *
         * @param context default context entries merged into each evaluation unless overridden
         * @return this builder
         */
        @NotNull
        public Builder context(@Nullable Map<String, ?> context) {
            if (context == null) {
                this.context = null;
                return this;
            }
            this.context = new HashMap<>();
            for (Map.Entry<String, ?> e : context.entrySet()) {
                this.context.put(e.getKey(), e.getValue());
            }
            return this;
        }

        /**
         * Keys in the evaluation context whose raw values must not be sent over the wire (raw values are hashed).
         *
         * @param sensitiveContextProperties set of property names; null clears the set
         * @return this builder
         */
        @NotNull
        public Builder sensitiveContextProperties(@Nullable Set<String> sensitiveContextProperties) {
            this.sensitiveContextProperties.clear();
            if (sensitiveContextProperties != null) {
                this.sensitiveContextProperties.addAll(sensitiveContextProperties);
            }
            return this;
        }

        /**
         * Overrides HTTP endpoints, timeouts, retry behavior, and the {@link java.net.http.HttpClient} instance.
         *
         * @param networkConfig optional; if null, {@link NetworkConfig#builder()}{@code .build()} defaults are used
         * @return this builder
         */
        @NotNull
        public Builder networkConfig(@Nullable NetworkConfig networkConfig) {
            this.networkConfig = networkConfig;
            return this;
        }

        /**
         * Registers a listener (ignored if null). Order is preserved for notification callbacks.
         *
         * @param listener hook implementation; may be null (no-op)
         * @return this builder
         */
        @NotNull
        public Builder addListener(@Nullable SupaClientListener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
            return this;
        }

        /**
         * Validates required fields and returns an immutable configuration.
         *
         * @return immutable configuration
         * @throws IllegalStateException if {@code sdkKey} or {@code environment} is null or blank
         */
        @NotNull
        public SupaClientConfig build() {
            if (sdkKey == null || sdkKey.isBlank()) {
                throw new IllegalStateException("sdkKey is required");
            }
            if (environment == null || environment.isBlank()) {
                throw new IllegalStateException("environment is required");
            }
            return new SupaClientConfig(this);
        }
    }
}
