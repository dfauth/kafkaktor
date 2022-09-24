package com.github.dfauth.functional;

import com.github.dfauth.trycatch.DispatchHandler;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class Failure<T> implements Try<T> {
    private final Throwable t;

    public Failure(Throwable t) {
        this.t = t;
    }

    @Override
    public final <V> V dispatch(DispatchHandler<T, V> handler) {
        return handler.dispatch(this);
    }

    @Override
    public <R> Try<R> map(Function<T, R> f) {
        return Try.failure(t);
    }

    @Override
    public Try<T> accept(Consumer<T> c) {
        throw new IllegalStateException("cannot call accept on "+this);
    }

    @Override
    public Try<T> recover(Function<Throwable, T> f) {
        return new Success<>(f.apply(this.t));
    }

    @Override
    public <R> Try<R> flatMap(Function<T, Try<R>> f) {
        return map(f).toOptional().orElse(new Failure<>(t));
    }

    @Override
    public Optional<T> toOptional() {
        return Optional.empty();
    }

    @Override
    public boolean isFailure() {
        return true;
    }

    @Override
    public boolean isSuccess() {
        return !isFailure();
    }

    public Throwable exception() {
        return this.t;
    }

    public T throwException() throws Throwable {
        throw this.t;
    }
}
