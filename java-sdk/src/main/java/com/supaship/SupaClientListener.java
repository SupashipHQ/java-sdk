package com.supaship;

import java.util.List;
import java.util.Map;

/**
 * Optional hooks mirroring the JavaScript SDK plugin extension points (subset). All methods have
 * default no-op implementations; exceptions thrown from a listener are ignored so evaluation always proceeds.
 */
public interface SupaClientListener {

    /**
     * Called before a batch evaluation request is sent (after context merge).
     *
     * @param featureNames non-null list of features being requested
     * @param context      evaluation context that will be sent (sensitive values may already be hashed)
     */
    default void beforeGetFeatures(List<String> featureNames, Map<String, ?> context) {}

    /**
     * Called after a successful evaluation (HTTP 2xx and parsed body), before the future completes normally.
     *
     * @param result  map of feature name to variation (or fallback) as returned to the caller
     * @param context context used for this request after merge
     */
    default void afterGetFeatures(Map<String, Object> result, Map<String, ?> context) {}

    /**
     * Called immediately before the HTTP request is issued.
     *
     * @param url     request URL
     * @param body    JSON request body
     * @param headers header map (e.g. Content-Type, Authorization); mutable only if the implementation copies it
     */
    default void beforeRequest(String url, String body, Map<String, String> headers) {}

    /**
     * Called after the HTTP response headers and status are available.
     *
     * @param statusCode HTTP status of the response
     * @param durationMs elapsed time for the round trip in milliseconds
     */
    default void afterResponse(int statusCode, long durationMs) {}

    /**
     * Called after a failed attempt when retries may continue.
     *
     * @param attempt    1-based attempt number for this logical request
     * @param error      failure from the attempt
     * @param willRetry  {@code true} if another attempt will follow
     */
    default void onRetryAttempt(int attempt, Throwable error, boolean willRetry) {}

    /**
     * Called when evaluation failed and fallbacks or terminal failure handling runs.
     *
     * @param error   failure that caused fallbacks to be used (or terminal failure if not retrying)
     * @param context evaluation context used for the failed attempt
     */
    default void onError(Throwable error, Map<String, ?> context) {}

    /**
     * Called once per feature when a configured local fallback is returned instead of a remote value.
     *
     * @param featureName    feature for which a configured fallback was returned
     * @param fallbackValue  value from {@link SupaClientConfig#features()}
     * @param error          underlying error from the network or API
     */
    default void onFallbackUsed(String featureName, Object fallbackValue, Throwable error) {}

    /**
     * Called when the client default context changes or a per-request overlay is applied.
     *
     * @param previousContext snapshot before the update
     * @param newContext      snapshot after the update
     * @param reason          e.g. {@code "updateContext"} or {@code "request"} for per-request overlays
     */
    default void onContextUpdate(
            Map<String, Object> previousContext, Map<String, Object> newContext, String reason) {}
}
