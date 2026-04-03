package com.supaship;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("resource")
class SupaClientHttpTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startWithHandler(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/features", handler);
        server.start();
        int port = server.getAddress().getPort();
        return "http://127.0.0.1:" + port + "/v1/features";
    }

    @Test
    void getFeatures_postsBearerTokenAndReturnsVariations() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        String baseUrl =
                startWithHandler(
                        ex -> {
                            if (!"POST".equals(ex.getRequestMethod())) {
                                ex.sendResponseHeaders(405, -1);
                                ex.close();
                                return;
                            }
                            authorization.set(ex.getRequestHeaders().getFirst("Authorization"));
                            requestBody.set(
                                    new String(
                                            ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                            byte[] body =
                                    "{\"features\":{\"dark-mode\":{\"variation\":true}}}"
                                            .getBytes(StandardCharsets.UTF_8);
                            ex.getResponseHeaders().add("Content-Type", "application/json");
                            ex.sendResponseHeaders(200, body.length);
                            ex.getResponseBody().write(body);
                            ex.close();
                        });

        Map<String, Object> features = new HashMap<>();
        features.put("dark-mode", false);
        SupaClientConfig cfg =
                SupaClientConfig.builder()
                        .sdkKey("test-key")
                        .environment("staging")
                        .features(features)
                        .networkConfig(
                                NetworkConfig.builder()
                                        .featuresApiUrl(baseUrl)
                                        .retry(new RetryConfig(false, 1, 0))
                                        .build())
                        .build();
        SupaClient client = new SupaClient(cfg);
        Map<String, Object> out = client.getFeatures(List.of("dark-mode")).get();
        assertEquals(true, out.get("dark-mode"));
        String req = requestBody.get();
        assertNotNull(req);
        assertEquals(
                "{\"environment\":\"staging\",\"features\":[\"dark-mode\"],\"context\":{}}", req);
        assertNotNull(authorization.get());
        assertTrue(
                authorization.get().contains("Bearer test-key"),
                () -> "Authorization: " + authorization.get());
    }

    @Test
    void fallsBackWhenHttpFails() throws Exception {
        String baseUrl =
                startWithHandler(
                        ex -> {
                            ex.sendResponseHeaders(503, -1);
                            ex.close();
                        });

        Map<String, Object> features = new HashMap<>();
        features.put("x", false);
        SupaClientConfig cfg =
                SupaClientConfig.builder()
                        .sdkKey("k")
                        .environment("e")
                        .features(features)
                        .networkConfig(
                                NetworkConfig.builder()
                                        .featuresApiUrl(baseUrl)
                                        .retry(new RetryConfig(false, 1, 0))
                                        .build())
                        .build();
        SupaClient client = new SupaClient(cfg);
        Map<String, Object> out = client.getFeatures(List.of("x")).get();
        assertEquals(false, out.get("x"));
    }

    @Test
    void retries_failed_requests() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        String baseUrl =
                startWithHandler(
                        ex -> {
                            int h = hits.incrementAndGet();
                            if (h < 2) {
                                ex.sendResponseHeaders(500, -1);
                                ex.close();
                                return;
                            }
                            byte[] body =
                                    "{\"features\":{\"f\":{\"variation\":true}}}"
                                            .getBytes(StandardCharsets.UTF_8);
                            ex.sendResponseHeaders(200, body.length);
                            ex.getResponseBody().write(body);
                            ex.close();
                        });

        Map<String, Object> features = Map.of("f", false);
        SupaClientConfig cfg =
                SupaClientConfig.builder()
                        .sdkKey("k")
                        .environment("e")
                        .features(features)
                        .networkConfig(
                                NetworkConfig.builder()
                                        .featuresApiUrl(baseUrl)
                                        .retry(new RetryConfig(true, 3, 1L))
                                        .build())
                        .build();
        SupaClient client = new SupaClient(cfg);
        assertEquals(true, client.getFeature("f").get());
        assertEquals(2, hits.get());
    }

    @Test
    void hashes_sensitive_context_fields() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        String baseUrl =
                startWithHandler(
                        ex -> {
                            requestBody.set(
                                    new String(
                                            ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                            byte[] body =
                                    "{\"features\":{\"f\":{\"variation\":1}}}"
                                            .getBytes(StandardCharsets.UTF_8);
                            ex.sendResponseHeaders(200, body.length);
                            ex.getResponseBody().write(body);
                            ex.close();
                        });

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("email", "secret@test");
        Map<String, Object> features = Map.of("f", 0);
        SupaClientConfig cfg =
                SupaClientConfig.builder()
                        .sdkKey("k")
                        .environment("e")
                        .features(features)
                        .context(ctx)
                        .sensitiveContextProperties(java.util.Set.of("email"))
                        .networkConfig(
                                NetworkConfig.builder()
                                        .featuresApiUrl(baseUrl)
                                        .retry(new RetryConfig(false, 1, 0))
                                        .build())
                        .build();
        SupaClient client = new SupaClient(cfg);
        assertEquals(1L, client.getFeature("f").get());
        String req = requestBody.get();
        assertNotNull(req);
        assertTrue(
                req.contains("544f5a10f875f4db5c5faef39c35d9a5b51123eeb5019e8bf1c65cb0b3d01cd9"),
                req);
    }
}
