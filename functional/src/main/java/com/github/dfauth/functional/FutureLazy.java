package com.github.dfauth.functional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("ConstantConditions")
public class FutureLazy<T> implements Lazy<T> {

    private final Supplier<T> supplier;
    private final CompletableFuture<T> fut;

    public FutureLazy(Supplier<T> supplier) {
        this(supplier, new CompletableFuture<>());
    }

    public FutureLazy(Supplier<T> supplier, CompletableFuture<T> fut) {
        this.supplier = requireNonNull(supplier);
        this.fut = requireNonNull(fut);
    }

    public <R> Lazy<R> map(Function<T,R> f) {
        return new FutureLazy<>(() -> f.apply(get()), fut.thenApply(f));
    }

    @Override
    public T get() {
        return Optional.ofNullable(fut)
                .filter(CompletableFuture::isDone)
                .map(_f -> tryCatch(_f::get))
                .orElseGet(() -> {
                    fut.complete(supplier.get());
                    return tryCatch(fut::get);
                });
    }
}
