package com.github.dfauth.functional;

import com.github.dfauth.trycatch.CallableFunction;
import com.github.dfauth.trycatch.ExceptionalRunnable;

import java.util.function.Consumer;

import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;

public enum Unit {
    UNIT;

    public static Unit of(ExceptionalRunnable r) {
        r.run();
        return UNIT;
    }

    public static <T,R> java.util.function.Function<T, R> toFunction(Consumer<T> consumer) {
        return t -> {
            consumer.accept(t);
            return null;
        };
    }

    public interface Function<I> extends java.util.function.Function<I,Unit>, Consumer<I> {
        static <I> Function<I> of(Runnable r) {
            return i -> r.run();
        }

        static <I,O> Function<I> of(java.util.function.Function<I, O> f) {
            return f::apply;
        }

        static <I,O> Function<I> of(CallableFunction<I, O> f) {
            return t -> tryCatch(() -> f.apply(t).call());
        }

        static <I> Function<I> of(Consumer<I> c) {
            return c::accept;
        }

        static Function<Unit> of(ExceptionalRunnable r) {
            return ignored -> r.run();
        }

        @Override
        default Unit apply(I i) {
            accept(i);
            return UNIT;
        }

    }
}
