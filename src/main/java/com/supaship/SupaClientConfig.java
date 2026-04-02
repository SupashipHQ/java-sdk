package com.supaship;

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

    public static Builder builder() {
        return new Builder();
    }

    public String sdkKey() {
        return sdkKey;
    }

    public String environment() {
        return environment;
    }

    /** Fallback values keyed by feature name (same role as {@code features} in the JS SDK). */
    public Map<String, Object> features() {
        return features;
    }

    /** Default evaluation context merged into each request unless overridden per call. */
    public Map<String, Object> context() {
        return context;
    }

    public Set<String> sensitiveContextProperties() {
        return sensitiveContextProperties;
    }

    public NetworkConfig networkConfig() {
        return networkConfig;
    }

    public List<SupaClientListener> listeners() {
        return listeners;
    }

    public static final class Builder {

        private String sdkKey;
        private String environment;
        private Map<String, Object> features = new HashMap<>();
        private Map<String, Object> context;
        private Set<String> sensitiveContextProperties = new HashSet<>();
        private NetworkConfig networkConfig;
        private final List<SupaClientListener> listeners = new ArrayList<>();

        public Builder sdkKey(String sdkKey) {
            this.sdkKey = sdkKey;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder features(Map<String, ?> features) {
            this.features.clear();
            if (features != null) {
                for (Map.Entry<String, ?> e : features.entrySet()) {
                    this.features.put(e.getKey(), e.getValue());
                }
            }
            return this;
        }

        public Builder context(Map<String, ?> context) {
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

        public Builder sensitiveContextProperties(Set<String> sensitiveContextProperties) {
            this.sensitiveContextProperties.clear();
            if (sensitiveContextProperties != null) {
                this.sensitiveContextProperties.addAll(sensitiveContextProperties);
            }
            return this;
        }

        public Builder networkConfig(NetworkConfig networkConfig) {
            this.networkConfig = networkConfig;
            return this;
        }

        public Builder addListener(SupaClientListener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
            return this;
        }

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
