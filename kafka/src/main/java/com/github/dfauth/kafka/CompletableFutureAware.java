package com.github.dfauth.kafka;

import com.github.dfauth.trycatch.ExceptionalConsumer;
import com.github.dfauth.trycatch.TryCatch;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;


public interface CompletableFutureAware<T,R> {

    static <T,R> Function<R, CompletableFuture<T>> runProvidingFuture(CompletableFutureAware<T,R> fAware) {
        CompletableFuture<T> f = new CompletableFuture<>();
        return r -> tryCatch(() -> {
            fAware.withCompletableFuture(f).accept(r);
            return f;
        }, e -> {
            if(!f.isCompletedExceptionally()) {
                f.completeExceptionally(e);
            }
            return f;
        });
    }

    static <T> Consumer<T> withCompletableFuture(CompletableFuture<T> f, Consumer<T> c) {
        return withCompletableFuture(f, c, Function.identity());
    }

    static <T,R> Consumer<T> withCompletableFuture(CompletableFuture<R> f, Consumer<T> c, Function<T,R> mapper) {
        return t -> withCompletableFuture(f, _ignored -> {
            c.accept(t);
            return mapper.apply(t);
        }).apply(t);
    }

    static <T,R> Function<T,R> withCompletableFuture(CompletableFuture<R> f, Function<T,R> fn) {
        return t ->
            TryCatch.Builder.tryCatch(() -> {
                R r = fn.apply(t);
                f.complete(r);
                return r;
            }).handleWith(_t -> {
                f.completeExceptionally(_t);
                throw new RuntimeException(_t);
            }).run();
    }

    ExceptionalConsumer<R> withCompletableFuture(CompletableFuture<T> f);
}