package com.github.dfauth.trycatch;

import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import static com.github.dfauth.functional.Unit.UNIT;
import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;

public class AsyncUtil {

    public static <T,R> Function<T,CompletableFuture<R>> async(Function<T,R> f) {
        return async(f, ForkJoinPool.commonPool());
    }

    public static <T,R> Function<T,CompletableFuture<R>> async(Function<T, R> f, ExecutorService executor) {
        return t -> executeAsync(() -> f.apply(t), executor);
    }

    public static CompletableFuture<Void> executeAsync(ExceptionalRunnable runnable) {
        return executeAsync(() -> {
            tryCatch(runnable, t -> UNIT);
            return UNIT;
        });
    }

    public static <T> CompletableFuture<T> executeAsync(Callable<T> callable) {
        return executeAsync(callable, ForkJoinPool.commonPool());
    }

    public static <T> CompletableFuture<T> executeAsync(Callable<T> callable, ExecutorService executor) {
        CompletableFuture<T> f = new CompletableFuture<>();
        executor.submit(() -> TryCatch._Runnable.tryCatch(() -> {
            T result = callable.call();
            f.complete(result);
        }, f::completeExceptionally));
        return f;
    }

    public static TimerTask asTimerTask(Runnable r) {
        return new TimerTask() {
            @Override
            public void run() {
                r.run();
            }
        };
    }
}
