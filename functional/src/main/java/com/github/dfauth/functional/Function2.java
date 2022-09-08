package com.github.dfauth.functional;

import java.util.function.*;

import static com.github.dfauth.trycatch.TryCatch.RunnableBuilder.ignoringConsumer;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;

public interface Function2<T,U,V> extends BiFunction<T,U,V> {

    static <T,U,V> Function2<T,U,V> asFunction2(BiFunction<T,U,V> f) {
        return (t,u) -> f.apply(t,u);
    }

    static <T,U,V> Function2<T,U,V> uncurry(Function<T,Function<U,V>> f) {
        return (t,u) -> f.apply(t).apply(u);
    }

    static <T> Predicate<T> toPredicate(Function<T, Boolean> f) {
        return t -> f.apply(t);
    }

    static <T> UnaryOperator<T> peek(Consumer<T> c) {
        return t -> {
            tryCatch(() -> c.accept(t), ignoringConsumer);
            return t;
        };
    }

    default Function<T, Function<U,V>> curried() {
        return t -> u -> apply(t,u);
    }

    default Function<T, Function<U,V>> curriedLeft() {
        return curried();
    }

    default Function<U, Function<T,V>> curriedRight() {
        return flip().curried();
    }

    default Function2<U,T,V> flip() {
        return (u,t) -> apply(t,u);
    }

}
