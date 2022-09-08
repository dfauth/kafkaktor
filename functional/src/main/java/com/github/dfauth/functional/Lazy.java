package com.github.dfauth.functional;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Lazy<T> extends Callable<T> {

    static <T> Lazy<T> lazy(Supplier<T> supplier) {
        return new FutureLazy<>(supplier);
    }

    <R> Lazy<R> map(Function<T,R> f);

    @Override
    default T call() {
        return get();
    }

    T get();
}
