package com.supaship;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * {@link EvaluateTransport} using {@link HttpURLConnection}. Suitable for Android (no {@code java.net.http}
 * requirement) when work runs off the UI thread (use {@link #AndroidEvaluateTransport(Executor)} with your app
 * executor).
 */
public final class AndroidEvaluateTransport implements EvaluateTransport {

    private final Executor executor;

    /** Uses {@link ForkJoinPool#commonPool()} for async completion (avoid blocking the main thread). */
    public AndroidEvaluateTransport() {
        this(ForkJoinPool.commonPool());
    }

    public AndroidEvaluateTransport(@NotNull Executor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public @NotNull CompletableFuture<TransportResponse> post(
            @NotNull String url,
            @NotNull String body,
            @NotNull Map<String, String> headers,
            long timeoutMs) {
        CompletableFuture<TransportResponse> out = new CompletableFuture<>();
        executor.execute(
                () -> {
                    HttpURLConnection conn = null;
                    try {
                        conn = (HttpURLConnection) new URL(url).openConnection();
                        conn.setRequestMethod("POST");
                        int to = (int) Math.min(Math.max(1L, timeoutMs), Integer.MAX_VALUE);
                        conn.setConnectTimeout(to);
                        conn.setReadTimeout(to);
                        conn.setDoOutput(true);
                        for (Map.Entry<String, String> e : headers.entrySet()) {
                            conn.setRequestProperty(e.getKey(), e.getValue());
                        }
                        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(bytes);
                        }
                        int code = conn.getResponseCode();
                        InputStream stream =
                                code >= HttpURLConnection.HTTP_OK
                                                && code < HttpURLConnection.HTTP_MULT_CHOICE
                                        ? conn.getInputStream()
                                        : conn.getErrorStream();
                        String responseBody = readFully(stream);
                        out.complete(new TransportResponse(code, responseBody));
                    } catch (Exception e) {
                        out.completeExceptionally(e);
                    } finally {
                        if (conn != null) {
                            conn.disconnect();
                        }
                    }
                });
        return out;
    }

    private static String readFully(InputStream in) throws java.io.IOException {
        if (in == null) {
            return "";
        }
        try (InputStream input = in;
                ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192];
            int n;
            while ((n = input.read(b)) != -1) {
                buf.write(b, 0, n);
            }
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
