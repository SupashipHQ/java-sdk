package com.supaship;

import com.supaship.internal.AsyncRetry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Supaship feature-flag client aligned with the JavaScript {@code SupaClient}: evaluates flags via
 * the Supaship HTTP API with the same defaults (URLs, retry, timeout) and the same fallback rules
 * when the network fails.
 *
 * <p>Pass a platform {@link EvaluateTransport} ({@code java.net.http} on the JVM, {@link java.net.HttpURLConnection}-based
 * on Android, or a test double).
 *
 * <p>Safe to use from Kotlin; nullability is annotated for Kotlin interop.
 */
public final class SupaClient {

    private final String sdkKey;
    private final String environment;
    private final Map<String, Object> featureDefinitions;
    private final Object contextLock = new Object();
    private final Map<String, Object> defaultContext;
    private final Set<String> sensitiveContextProperties;
    private final NetworkSettings network;
    private final EvaluateTransport transport;
    private final List<SupaClientListener> listeners;
    private final String clientId;

    /**
     * @param config    non-null client configuration from {@link SupaClientConfig.Builder#build()}
     * @param transport non-null HTTP implementation for evaluate POSTs
     */
    public SupaClient(@NotNull SupaClientConfig config, @NotNull EvaluateTransport transport) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(transport, "transport");
        this.sdkKey = config.sdkKey();
        this.environment = config.environment();
        this.featureDefinitions = new HashMap<>(config.features());
        this.defaultContext = new HashMap<>(config.context());
        this.sensitiveContextProperties = config.sensitiveContextProperties();
        this.network = config.networkSettings();
        this.transport = transport;
        this.listeners = new ArrayList<>(config.listeners());
        this.clientId = generateClientId();
    }

    @NotNull
    public String clientId() {
        return clientId;
    }

    public void updateContext(@Nullable Map<String, ?> context, boolean mergeWithExisting) {
        Map<String, ?> toApply = context == null ? Map.of() : context;
        Map<String, Object> oldSnapshot;
        Map<String, Object> newSnapshot;
        synchronized (contextLock) {
            oldSnapshot = new HashMap<>(defaultContext);
            if (mergeWithExisting && !defaultContext.isEmpty()) {
                putAllNullable(defaultContext, toApply);
            } else {
                defaultContext.clear();
                putAllNullable(defaultContext, toApply);
            }
            newSnapshot = new HashMap<>(defaultContext);
        }
        for (SupaClientListener listener : listeners) {
            try {
                listener.onContextUpdate(oldSnapshot, newSnapshot, "updateContext");
            } catch (Throwable ignored) {
                //
            }
        }
    }

    @NotNull
    public Map<String, Object> getContext() {
        synchronized (contextLock) {
            return new HashMap<>(defaultContext);
        }
    }

    @Nullable
    public Object getFeatureFallback(@NotNull String featureName) {
        return featureDefinitions.get(featureName);
    }

    @NotNull
    public CompletableFuture<@Nullable Object> getFeature(@NotNull String featureName) {
        return getFeature(featureName, null);
    }

    @NotNull
    public CompletableFuture<@Nullable Object> getFeature(
            @NotNull String featureName, @Nullable Map<String, ?> contextOverride) {
        List<String> one = Collections.singletonList(featureName);
        return getFeatures(one, contextOverride).thenApply(m -> m.get(featureName));
    }

    @NotNull
    public CompletableFuture<@NotNull Map<String, Object>> getFeatures(
            @NotNull List<String> featureNames) {
        return getFeatures(featureNames, null);
    }

    @NotNull
    public CompletableFuture<@NotNull Map<String, Object>> getFeatures(
            @NotNull List<String> featureNames, @Nullable Map<String, ?> contextOverride) {
        List<String> names =
                featureNames.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (names.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        Map<String, Object> mergedContext = mergeContext(contextOverride);
        if (contextOverride != null && !contextOverride.isEmpty()) {
            Map<String, Object> defaultSnap = getContext();
            for (SupaClientListener listener : listeners) {
                try {
                    listener.onContextUpdate(defaultSnap, new HashMap<>(mergedContext), "request");
                } catch (Throwable ignored) {
                    //
                }
            }
        }

        for (SupaClientListener listener : listeners) {
            try {
                listener.beforeGetFeatures(Collections.unmodifiableList(names), mergedContext);
            } catch (Throwable ignored) {
                //
            }
        }

        Map<String, Object> contextForRequest;
        try {
            contextForRequest = hashSensitiveContext(mergedContext);
        } catch (NoSuchAlgorithmException e) {
            return CompletableFuture.failedFuture(
                    new SupashipException("SHA-256 not available", e));
        }

        RetryConfig retry = network.retry();
        CompletableFuture<Map<String, Object>> evaluated =
                AsyncRetry.runWithRetry(
                        attempt -> executeEvaluate(names, contextForRequest),
                        retry.maxAttempts(),
                        retry.backoffMs(),
                        retry.enabled(),
                        ev -> {
                            for (SupaClientListener listener : listeners) {
                                try {
                                    listener.onRetryAttempt(
                                            ev.attempt(), ev.error(), ev.willRetry());
                                } catch (Throwable ignored) {
                                    //
                                }
                            }
                        },
                        null);

        return evaluated.handle(
                (result, error) -> {
                    if (error == null) {
                        for (SupaClientListener listener : listeners) {
                            try {
                                listener.afterGetFeatures(result, mergedContext);
                            } catch (Throwable ignored) {
                                //
                            }
                        }
                        return result;
                    }
                    for (SupaClientListener listener : listeners) {
                        try {
                            listener.onError(error, mergedContext);
                        } catch (Throwable ignored) {
                            //
                        }
                    }
                    Map<String, Object> fallbacks = new LinkedHashMap<>();
                    for (String name : names) {
                        Object fb = featureDefinitions.get(name);
                        fallbacks.put(name, fb);
                        for (SupaClientListener listener : listeners) {
                            try {
                                listener.onFallbackUsed(name, fb, error);
                            } catch (Throwable ignored) {
                                //
                            }
                        }
                    }
                    return fallbacks;
                });
    }

    private CompletableFuture<Map<String, Object>> executeEvaluate(
            List<String> featureNames, Map<String, Object> contextForRequest) {
        String url = network.featuresApiUrl();
        String body =
                FeatureEvaluateJson.buildEvaluateRequest(environment, featureNames, contextForRequest);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + sdkKey);
        for (SupaClientListener listener : listeners) {
            try {
                listener.beforeRequest(url, body, headers);
            } catch (Throwable ignored) {
                //
            }
        }

        long timeoutMs = network.requestTimeoutMs();
        long startNs = System.nanoTime();
        return transport
                .post(url, body, headers, timeoutMs)
                .thenApply(
                        response -> {
                            long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
                            for (SupaClientListener listener : listeners) {
                                try {
                                    listener.afterResponse(response.statusCode(), durationMs);
                                } catch (Throwable ignored) {
                                    //
                                }
                            }
                            if (response.statusCode() / 100 != 2) {
                                throw new SupashipException(
                                        response.statusCode(),
                                        "Failed to fetch features: HTTP "
                                                + response.statusCode());
                            }
                            Map<String, Object> parsed =
                                    FeatureEvaluateJson.parseEvaluateResponse(response.body());
                            Map<String, Object> result = new LinkedHashMap<>();
                            for (String name : featureNames) {
                                Object variation = parsed.get(name);
                                result.put(
                                        name,
                                        resolveVariation(
                                                variation, featureDefinitions.get(name)));
                            }
                            return result;
                        });
    }

    private static Object resolveVariation(Object variation, Object fallback) {
        if (variation != null) {
            return variation;
        }
        return fallback;
    }

    private Map<String, Object> mergeContext(Map<String, ?> contextOverride) {
        synchronized (contextLock) {
            if (contextOverride == null) {
                return new HashMap<>(defaultContext);
            }
            Map<String, Object> merged = new HashMap<>(defaultContext);
            putAllNullable(merged, contextOverride);
            return merged;
        }
    }

    private Map<String, Object> hashSensitiveContext(Map<String, Object> context)
            throws NoSuchAlgorithmException {
        if (context == null
                || context.isEmpty()
                || sensitiveContextProperties == null
                || sensitiveContextProperties.isEmpty()) {
            return context;
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        Map<String, Object> out = new HashMap<>(context);
        for (String key : sensitiveContextProperties) {
            if (!out.containsKey(key)) {
                continue;
            }
            Object val = out.get(key);
            if (val == null) {
                continue;
            }
            md.reset();
            md.update(String.valueOf(val).getBytes(StandardCharsets.UTF_8));
            out.put(key, toHex(md.digest()));
        }
        return out;
    }

    private static void putAllNullable(Map<String, Object> dest, Map<String, ?> src) {
        for (Map.Entry<String, ?> e : src.entrySet()) {
            dest.put(e.getKey(), e.getValue());
        }
    }

    private static String toHex(byte[] digest) {
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String generateClientId() {
        String suffix =
                Long.toString(ThreadLocalRandom.current().nextLong() & 0x1ffffffffffL, 36);
        if (suffix.length() > 7) {
            suffix = suffix.substring(0, 7);
        }
        return "supaship-" + System.currentTimeMillis() + "-" + suffix;
    }
}
