package com.github.dfauth.functional;

import com.github.dfauth.trycatch.DispatchHandler;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;

public class Success<T> implements Try<T> {

    private final T result;

    public Success(T t) {
        result = t;
    }

    @Override
    public Try<T> recover(Function<Throwable, T> f) {
        return this;
    }

    @Override
    public final <V> V dispatch(DispatchHandler<T,V> handler) {
        return handler.dispatch(this);
    }

    @Override
    public <R> Try<R> map(Function<T, R> f) {
        return Try.tryWithCallable(() -> f.apply(result));
    }

    @Override
    public Try<T> accept(Consumer<T> c) {
        tryCatch(() -> c.accept(this.result));
        return this;
    }

    @Override
    public <R> Try<R> flatMap(Function<T, Try<R>> f) {
        return map(f).toOptional().orElseGet(() -> toFailure().flatMap(f));
    }

    @Override
    public Optional<T> toOptional() {
        return Optional.ofNullable(result);
    }

    @Override
    public boolean isFailure() {
        return !isSuccess();
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    public T result() {
        return result;
    }
}
