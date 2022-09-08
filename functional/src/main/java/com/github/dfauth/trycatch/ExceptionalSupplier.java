package com.github.dfauth.trycatch;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

@FunctionalInterface
public interface ExceptionalSupplier<T> extends Supplier<T>, Callable<T> {

    static <T> Supplier<T> asSupplier(ExceptionalSupplier<T> s) {
        return s::get;
    }

    default T call() throws Exception {
        return _get();
    }

    default T get() {
        try {
            return _get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    T _get() throws Exception;
}
