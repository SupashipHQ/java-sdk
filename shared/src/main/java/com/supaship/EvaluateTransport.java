package com.supaship;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Pluggable HTTP POST for feature evaluation (Java {@code HttpClient}, Android URLConnection, tests, etc.). */
public interface EvaluateTransport {

    /**
     * POST {@code body} to {@code url} with {@code headers} and client-side timeouts derived from {@code timeoutMs}.
     *
     * @param url        evaluate API URL
     * @param body       JSON request body
     * @param headers    header names and values (e.g. Content-Type, Authorization)
     * @param timeoutMs  upper bound for this attempt (connect + response)
     * @return future completed with status and body, or exceptionally on failure
     */
    CompletableFuture<TransportResponse> post(
            String url, String body, Map<String, String> headers, long timeoutMs);
}
