package com.github.dfauth.trycatch;

import com.github.dfauth.functional.Unit;

import java.util.function.Consumer;

import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;

public interface ExceptionalConsumer<T> extends java.util.function.Consumer<T>, CallableFunction<T, Void> {

    static <T, R> Consumer<T> toConsumer(CallableFunction<T, R> f) {
        return Unit.Function.of(f);
    }

    default void accept(T t) {
        tryCatch(() -> _accept(t)); // converts checked exception to runtime
    }

    default Void _apply(T t) throws Exception {
        _accept(t);
        return null;
    }

    void _accept(T t) throws Exception;
}
