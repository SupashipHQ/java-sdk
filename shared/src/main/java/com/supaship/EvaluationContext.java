package com.supaship;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default evaluation context for {@link SupashipClientConfig} (key–value map sent with feature requests).
 * Prefer this over raw maps for clearer, structured construction.
 */
public final class EvaluationContext {

    private final Map<String, Object> entries;

    private EvaluationContext(Map<String, Object> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    /**
     * A single attribute (common case).
     */
    @NotNull
    public static EvaluationContext of(@NotNull String key, @Nullable Object value) {
        Objects.requireNonNull(key, "key");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(key, value);
        return new EvaluationContext(m);
    }

    /**
     * Wraps an existing map (defensive copy). Use {@link #of} or {@link #builder} when building context in code.
     */
    @NotNull
    public static EvaluationContext fromMap(@Nullable Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return new EvaluationContext(Collections.emptyMap());
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, ?> e : map.entrySet()) {
            copy.put(e.getKey(), e.getValue());
        }
        return new EvaluationContext(copy);
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Snapshot as a map (for example {@link SupashipClientConfig.Builder#contextMap(Map)}). Do not mutate the
     * returned map.
     */
    @NotNull
    public Map<String, Object> asMap() {
        return entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EvaluationContext that = (EvaluationContext) o;
        return entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    public static final class Builder {

        private final Map<String, Object> map = new LinkedHashMap<>();

        @NotNull
        public Builder userId(@Nullable String userId) {
            if (userId != null) {
                map.put("userId", userId);
            }
            return this;
        }

        @NotNull
        public Builder attribute(@NotNull String key, @Nullable Object value) {
            Objects.requireNonNull(key, "key");
            map.put(key, value);
            return this;
        }

        @NotNull
        public EvaluationContext build() {
            return new EvaluationContext(map);
        }
    }
}
