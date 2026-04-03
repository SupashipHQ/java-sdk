package com.supaship;

import java.util.List;
import java.util.Map;

/**
 * Optional hooks mirroring the JavaScript SDK plugin extension points (subset). All methods have
 * default no-op implementations.
 */
public interface SupaClientListener {

    default void beforeGetFeatures(List<String> featureNames, Map<String, ?> context) {}

    default void afterGetFeatures(Map<String, Object> result, Map<String, ?> context) {}

    default void beforeRequest(String url, String body, Map<String, String> headers) {}

    default void afterResponse(int statusCode, long durationMs) {}

    default void onRetryAttempt(int attempt, Throwable error, boolean willRetry) {}

    default void onError(Throwable error, Map<String, ?> context) {}

    default void onFallbackUsed(String featureName, Object fallbackValue, Throwable error) {}

    default void onContextUpdate(
            Map<String, Object> previousContext, Map<String, Object> newContext, String reason) {}
}
