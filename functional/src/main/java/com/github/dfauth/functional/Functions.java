package com.github.dfauth.functional;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.*;

import static com.github.dfauth.functional.Tuple2.tuple2;
import static com.github.dfauth.trycatch.TryCatch.RunnableBuilder.ignoringConsumer;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;

public class Functions {

    public static <T> Predicate<T> toPredicate(Function<T, Boolean> f) {
        return f::apply;
    }

    public static <T> UnaryOperator<T> peek(Consumer<T> c) {
        return t -> {
            tryCatch(() -> c.accept(t), ignoringConsumer);
            return t;
        };
    }

    public static <T,R> BiFunction<T,R,Tuple2<T,R>> peek(BiConsumer<T,R> c) {
        return (t,r) -> {
            tryCatch(() -> c.accept(t,r), ignoringConsumer);
            return tuple2(t,r);
        };
    }

    public static <T,R> BiFunction<T,R,T> peekLeft(BiConsumer<T,R> c) {
        return peek(c).andThen(Tuple2::_1);
    }

    public static <T,R> BiFunction<T,R,R> peekRight(BiConsumer<T,R> c) {
        return peek(c).andThen(Tuple2::_2);
    }

    public static <K,V,T> Function<Map.Entry<K,V>,Map.Entry<K,T>> mapEntry(Function<Map.Entry<K, V>, T> f) {
        return e -> Map.entry(e.getKey(), f.apply(e));
    }

    public static <K,V,T> Function<Map.Entry<K,V>,Map.Entry<K,T>> mapEntryValue(Function<V, T> f) {
        return mapEntry(e -> f.apply(e.getValue()));
    }

    public static <K,V> Function<K,Map.Entry<K,V>> toMapEntry(Function<K, V> f) {
        return k -> Map.entry(k, f.apply(k));
    }

    public static <S,T> Function<S,Consumer<T>> ignore(Consumer<T> c) {
        return s -> c;
    }

    public static <S,T,R> Function<S,Function<T,R>> ignore(Function<T,R> f) {
        return s -> f;
    }

    public static <S,T> Function<S,T> ignoreT(T t) {
        return s -> t;
    }

    public static Callable<Void> toCallable(Runnable r) {
        return () -> {
            r.run();
            return null;
        };
    }
}
