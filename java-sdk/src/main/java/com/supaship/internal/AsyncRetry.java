package com.supaship.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * Exponential backoff retry for asynchronous work (aligned with the JavaScript SDK retry helper).
 *
 * <p>When retries are disabled or {@code maxAttempts &lt; 1}, the first attempt is run once with no delay.
 */
public final class AsyncRetry {

    private AsyncRetry() {}

    /**
     * Runs {@code task} at least once; on failure, waits {@code baseBackoffMs * 2^(attempt-1)} before the next try
     * until {@code maxAttempts} is reached.
     *
     * @param <T>            result type
     * @param task           supplier of a future for each attempt; receives 1-based attempt number
     * @param maxAttempts    maximum number of tries when {@code enabled} ({@code >= 1})
     * @param baseBackoffMs  base delay in milliseconds (doubled after each failure)
     * @param enabled        when {@code false}, only the first attempt runs
     * @param onAttempt      optional listener invoked after each failure with retry metadata; may be {@code null}
     * @param executor       executor used for scheduling delayed retries; {@code null} uses {@link ForkJoinPool#commonPool()}
     * @return future that completes with the task result or exceptionally with the last error
     */
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

    /**
     * Produces the async work for a single attempt.
     *
     * @param <T> result type of the {@link CompletableFuture}
     */
    @FunctionalInterface
    public interface RetryTask<T> {
        /**
         * Runs one logical try (for example one HTTP call).
         *
         * @param attemptNumber 1-based index (first try is {@code 1})
         * @return future for this attempt
         */
        CompletableFuture<T> run(int attemptNumber);
    }

    /** Details supplied to {@code onAttempt} after a failed try. */
    public static final class RetryEvent {
        private final int attempt;
        private final Throwable error;
        private final boolean willRetry;

        /**
         * Immutable snapshot passed to retry listeners after a failed try.
         *
         * @param attempt   1-based attempt that failed
         * @param error     failure from the attempt
         * @param willRetry {@code true} if another attempt is scheduled
         */
        public RetryEvent(int attempt, Throwable error, boolean willRetry) {
            this.attempt = attempt;
            this.error = error;
            this.willRetry = willRetry;
        }

        /**
         * Which try in the sequence failed.
         *
         * @return 1-based attempt number that produced {@link #error()}
         */
        public int attempt() {
            return attempt;
        }

        /**
         * Failure that triggered this notification.
         *
         * @return the throwable from the failed attempt
         */
        public Throwable error() {
            return error;
        }

        /**
         * Indicates whether {@link AsyncRetry#runWithRetry} will schedule another attempt.
         *
         * @return whether a follow-up attempt will run
         */
        public boolean willRetry() {
            return willRetry;
        }
    }
}
