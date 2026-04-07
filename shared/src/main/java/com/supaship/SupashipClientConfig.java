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

/** Configuration for {@link SupashipClient}. Immutable after {@link Builder#build()}. */
public final class SupashipClientConfig {

    private final String sdkKey;
    private final String environment;
    private final Map<String, Object> features;
    private final Map<String, Object> context;
    private final Set<String> sensitiveContextProperties;
    private final NetworkSettings networkSettings;
    private final List<SupashipClientListener> listeners;

    private SupashipClientConfig(Builder b) {
        this.sdkKey = b.sdkKey;
        this.environment = b.environment;
        this.features = Collections.unmodifiableMap(new HashMap<>(b.features));
        this.context =
                b.context == null
                        ? Collections.emptyMap()
                        : Collections.unmodifiableMap(new HashMap<>(b.context));
        this.sensitiveContextProperties =
                Collections.unmodifiableSet(new HashSet<>(b.sensitiveContextProperties));
        this.networkSettings =
                b.networkSettings != null ? b.networkSettings : NetworkSettings.builder().build();
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

    @NotNull
    public String sdkKey() {
        return sdkKey;
    }

    @NotNull
    public String environment() {
        return environment;
    }

    @NotNull
    public Map<String, Object> features() {
        return features;
    }

    @NotNull
    public Map<String, Object> context() {
        return context;
    }

    @NotNull
    public Set<String> sensitiveContextProperties() {
        return sensitiveContextProperties;
    }

    /**
     * Endpoints, timeouts, and retry policy for evaluation HTTP calls. Transport implementation is chosen when
     * constructing {@link SupashipClient} (for example {@code JavaEvaluateTransport} or {@code AndroidEvaluateTransport}).
     */
    @NotNull
    public NetworkSettings networkSettings() {
        return networkSettings;
    }

    @NotNull
    public List<SupashipClientListener> listeners() {
        return listeners;
    }

    /** Fluent builder for {@link SupashipClientConfig}; {@link #build()} validates required fields. */
    public static final class Builder {

        public Builder() {}

        private String sdkKey;
        private String environment;
        private Map<String, Object> features = new HashMap<>();
        private Map<String, Object> context;
        private Set<String> sensitiveContextProperties = new HashSet<>();
        private NetworkSettings networkSettings;
        private final List<SupashipClientListener> listeners = new ArrayList<>();

        @NotNull
        public Builder sdkKey(@Nullable String sdkKey) {
            this.sdkKey = sdkKey;
            return this;
        }

        @NotNull
        public Builder environment(@Nullable String environment) {
            this.environment = environment;
            return this;
        }

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

        @NotNull
        public Builder sensitiveContextProperties(@Nullable Set<String> sensitiveContextProperties) {
            this.sensitiveContextProperties.clear();
            if (sensitiveContextProperties != null) {
                this.sensitiveContextProperties.addAll(sensitiveContextProperties);
            }
            return this;
        }

        /**
         * Network settings for evaluate requests. When omitted, {@link NetworkSettings#builder()}{@code .build()}
         * defaults apply.
         */
        @NotNull
        public Builder networkSettings(@Nullable NetworkSettings networkSettings) {
            this.networkSettings = networkSettings;
            return this;
        }

        @NotNull
        public Builder addListener(@Nullable SupashipClientListener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
            return this;
        }

        @NotNull
        public SupashipClientConfig build() {
            if (sdkKey == null || sdkKey.isBlank()) {
                throw new IllegalStateException("sdkKey is required");
            }
            if (environment == null || environment.isBlank()) {
                throw new IllegalStateException("environment is required");
            }
            return new SupashipClientConfig(this);
        }
    }
}
