package com.supaship;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Typed builder for feature fallback values ({@link SupashipClientConfig#features()}). Supaship serves
 * <strong>boolean</strong> or <strong>JSON</strong> variations only; fallbacks must use the same shapes:
 * {@code null}, {@link Boolean}, JSON primitives as {@link String} or {@link Number}, JSON arrays as
 * {@link List}, JSON objects as {@link Map} with string keys (values validated recursively).
 */
public final class FeatureDefaults {

    private final Map<String, Object> flags;

    private FeatureDefaults(Map<String, Object> flags) {
        this.flags = Collections.unmodifiableMap(new LinkedHashMap<>(flags));
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Unmodifiable map; safe to pass to {@link SupashipClientConfig.Builder#features(Map)} or
     * {@link SupashipClientConfig.Builder#defaults(FeatureDefaults)}.
     */
    @NotNull
    public Map<String, Object> asMap() {
        return flags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureDefaults that = (FeatureDefaults) o;
        return flags.equals(that.flags);
    }

    @Override
    public int hashCode() {
        return flags.hashCode();
    }

    static void validateJsonFallback(@Nullable Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Boolean || value instanceof String || value instanceof Number) {
            return;
        }
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                validateJsonFallback(item);
            }
            return;
        }
        if (value instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                if (!(e.getKey() instanceof String)) {
                    throw new IllegalArgumentException(
                            "JSON object keys must be strings; got " + e.getKey());
                }
                validateJsonFallback(e.getValue());
            }
            return;
        }
        throw new IllegalArgumentException(
                "Unsupported type (Supaship allows boolean and JSON only): " + value.getClass().getName());
    }

    public static final class Builder {

        private final Map<String, Object> map = new LinkedHashMap<>();

        @NotNull
        public Builder feature(@NotNull String name, @Nullable Object value) {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("feature name must not be blank");
            }
            validateJsonFallback(value);
            map.put(name, value);
            return this;
        }

        @NotNull
        public FeatureDefaults build() {
            return new FeatureDefaults(map);
        }
    }
}
