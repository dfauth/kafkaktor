package com.github.dfauth.functional;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface Function2<A,B,C> extends BiFunction<A,B,C> {

    static <A,B,C> Function2<A,B,C> function2(BiFunction<A,B,C> f) {
        return f::apply;
    }

    static <A, B, C> Function2<A, B, C> function2(Function<A, Function<B, C>> f) {
        return uncurry(f);
    }

    static <A,B,C> Function2<A,B,C> uncurry(Function<A,Function<B,C>> f) {
        return (t,u) -> f.apply(t).apply(u);
    }

    default Function<A, Function<B,C>> curry() {
        return t -> u -> apply(t,u);
    }

    default Function<A, Function<B,C>> curryLeft() {
        return curry();
    }

    default Function<B, Function<A,C>> curryRight() {
        return flip().curry();
    }

    default Function2<B,A,C> flip() {
        return (u,t) -> apply(t,u);
    }

    default Function<A, Function<B, C>> unwind() {
        return curry();
    }
}
