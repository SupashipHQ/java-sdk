package com.supaship;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** {@link EvaluateTransport} backed by {@link HttpClient} (Java&nbsp;11+). */
public final class JavaEvaluateTransport implements EvaluateTransport {

    private final HttpClient httpClient;

    public JavaEvaluateTransport(@NotNull HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public @NotNull CompletableFuture<TransportResponse> post(
            @NotNull String url,
            @NotNull String body,
            @NotNull Map<String, String> headers,
            long timeoutMs) {
        HttpRequest.Builder rb =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofMillis(Math.max(1L, timeoutMs)))
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            rb.header(e.getKey(), e.getValue());
        }
        HttpRequest request = rb.build();
        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(r -> new TransportResponse(r.statusCode(), r.body()));
    }
}
