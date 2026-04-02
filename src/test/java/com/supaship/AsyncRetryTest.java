package com.supaship;

import com.supaship.internal.AsyncRetry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncRetryTest {

    @Test
    void retriesWithBackoff_untilSuccess() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        CompletableFuture<String> f =
                AsyncRetry.runWithRetry(
                        n ->
                                CompletableFuture.supplyAsync(
                                        () -> {
                                            int a = attempts.incrementAndGet();
                                            if (a < 3) {
                                                throw new RuntimeException("fail");
                                            }
                                            return "ok";
                                        }),
                        5,
                        5L,
                        true,
                        null,
                        null);
        assertEquals("ok", f.get());
        assertEquals(3, attempts.get());
    }

    @Test
    void notifies_listener_on_retry() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger retryEvents = new AtomicInteger();
        CompletableFuture<String> f =
                AsyncRetry.runWithRetry(
                        n ->
                                CompletableFuture.supplyAsync(
                                        () -> {
                                            if (attempts.incrementAndGet() < 2) {
                                                throw new RuntimeException("x");
                                            }
                                            return "done";
                                        }),
                        3,
                        1L,
                        true,
                        ev -> retryEvents.incrementAndGet(),
                        null);
        assertEquals("done", f.get());
        assertTrue(retryEvents.get() >= 1);
    }
}
