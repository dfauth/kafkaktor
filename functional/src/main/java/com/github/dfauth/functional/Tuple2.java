package com.github.dfauth.functional;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.github.dfauth.functional.Function2.uncurry;

public interface Tuple2<T1, T2> {

    static <T1,T2> Tuple2<T1, T2> of(T1 t1, T2 t2) {
        return new Tuple2<>() {
            @Override
            public T1 _1() {
                return t1;
            }

            @Override
            public T2 _2() {
                return t2;
            }
        };
    }

    static <T1,T2> Map.Entry<T1, T2> asMapEntry(T1 t1, T2 t2) {
        return of(t1, t2).toMapEntry();
    }

    T1 _1();

    T2 _2();

    default <T> T map(BiFunction<T1,T2,T> f) {
        return f.apply(_1(), _2());
    }

    default <T> T map(Function<T1,Function<T2,T>> f) {
        return map(uncurry(f));
    }

    default Map.Entry<T1,T2> toMapEntry() {
        return new Map.Entry<>(){
            @Override
            public T1 getKey() {
                return _1();
            }

            @Override
            public T2 getValue() {
                return _2();
            }

            @Override
            public T2 setValue(T2 value) {
                throw new UnsupportedOperationException();
            }
        };
    }

    default void forEach(BiConsumer<T1,T2> c) {
        c.accept(_1(), _2());
    }
}
