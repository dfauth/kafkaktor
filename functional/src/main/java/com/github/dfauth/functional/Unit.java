package com.github.dfauth.functional;

import com.github.dfauth.trycatch.CallableFunction;
import com.github.dfauth.trycatch.ExceptionalRunnable;

import java.util.function.Consumer;

import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;

public class Unit {
    public static final Void UNIT = null;

    public static Void of(ExceptionalRunnable r) {
        r.run();
        return UNIT;
    }

    public static <T> java.util.function.Function<T, Void> toFunction(Consumer<T> consumer) {
        return t -> {
            consumer.accept(t);
            return UNIT;
        };
    }

    public interface Function0<I> extends java.util.function.Function<I,Void>, Consumer<I> {
        static <I> Function0<I> of(Runnable r) {
            return i -> r.run();
        }

        static <I,O> Function0<I> of(java.util.function.Function<I, O> f) {
            return f::apply;
        }

        static <I,O> Function0<I> of(CallableFunction<I, O> f) {
            return t -> tryCatch(() -> f.apply(t).call());
        }

        static <I> Function0<I> of(Consumer<I> c) {
            return c::accept;
        }

        static Function0<Unit> of(ExceptionalRunnable r) {
            return ignored -> r.run();
        }

        @Override
        default Void apply(I i) {
            accept(i);
            return UNIT;
        }

    }
}
