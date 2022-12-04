package com.github.dfauth.functional;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface Function0<T> extends Function<Object,T>, Supplier<T> {

    static <T> Function0<T> function0(Function<?, T> f) {
        return () -> f.apply(null);
    }

    static <T> Function0<T> function0(Supplier<T> s) {
        return s::get;
    }

    static <T> Function0<T> function0(Runnable r) {
        r.run();
        return null;
    }

    @Override
    default T apply(Object unused) {
        return get();
    }
}
