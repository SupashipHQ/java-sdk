package com.supaship.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/** Exponential backoff retry for async operations (same shape as the JS SDK {@code retry} helper). */
public final class AsyncRetry {

    private AsyncRetry() {}

    public static <T> CompletableFuture<T> runWithRetry(
            RetryTask<T> task,
            int maxAttempts,
            long baseBackoffMs,
            boolean enabled,
            Consumer<RetryEvent> onAttempt,
            Executor executor) {
        if (!enabled || maxAttempts < 1) {
            return task.run(1).toCompletableFuture();
        }
        Executor ex = executor != null ? executor : ForkJoinPool.commonPool();
        CompletableFuture<T> result = new CompletableFuture<>();
        runAttempt(task, 1, maxAttempts, baseBackoffMs, onAttempt, ex, result);
        return result;
    }

    private static <T> void runAttempt(
            RetryTask<T> task,
            int attempt,
            int maxAttempts,
            long baseBackoffMs,
            Consumer<RetryEvent> onAttempt,
            Executor executor,
            CompletableFuture<T> result) {
        task.run(attempt)
                .whenComplete(
                        (value, error) -> {
                            if (error == null) {
                                result.complete(value);
                                return;
                            }
                            boolean willRetry = attempt < maxAttempts;
                            if (onAttempt != null) {
                                try {
                                    onAttempt.accept(new RetryEvent(attempt, error, willRetry));
                                } catch (Throwable ignored) {
                                    // listener must not break retry
                                }
                            }
                            if (!willRetry) {
                                result.completeExceptionally(error);
                                return;
                            }
                            long delay = baseBackoffMs * (1L << (attempt - 1));
                            CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS, executor)
                                    .execute(
                                            () ->
                                                    runAttempt(
                                                            task,
                                                            attempt + 1,
                                                            maxAttempts,
                                                            baseBackoffMs,
                                                            onAttempt,
                                                            executor,
                                                            result));
                        });
    }

    @FunctionalInterface
    public interface RetryTask<T> {
        CompletableFuture<T> run(int attemptNumber);
    }

    public static final class RetryEvent {
        private final int attempt;
        private final Throwable error;
        private final boolean willRetry;

        public RetryEvent(int attempt, Throwable error, boolean willRetry) {
            this.attempt = attempt;
            this.error = error;
            this.willRetry = willRetry;
        }

        public int attempt() {
            return attempt;
        }

        public Throwable error() {
            return error;
        }

        public boolean willRetry() {
            return willRetry;
        }
    }
}
