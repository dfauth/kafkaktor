package com.github.dfauth.functional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Maps {

    static <K,V,T> Function<Map<K,V>,Map<K,T>> mapTransformerOf(BiFunction<K,V,T> f) {
        return mapTransformerOf(e -> Tuple2.of(e.getKey(), f.apply(e.getKey(), e.getValue())).toMapEntry());
    }

    static <K,V,T,R> Function<Map<K,V>,Map<T,R>> mapTransformerOf(Function<Map.Entry<K, V>, Map.Entry<T,R>> f) {
        return m -> m.entrySet().stream().map(f).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static <T, V, K> Function<Map.Entry<K,V>, T> mapEntryTransformer(BiFunction<K,V,T> f) {
        return e -> f.apply(e.getKey(), e.getValue());
    }

    @SafeVarargs
    static <K, V> Map<K,V> concat(Map<K, V>... maps) {
        return concat((v1,v2) -> v2, maps);
    }

    @SafeVarargs
    static <K, V> Map<K,V> concat(BinaryOperator<V> mergeFunction, Map<K, V>... maps) {
        return Stream.of(maps).flatMap(m -> m.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, mergeFunction));
    }

    static <K,V,T> Map<K,T> map(Map<K, V> m, BiFunction<K,V,T> f) {
        return m.entrySet().stream().map(
                e -> Tuple2.tuple2(e).map(f.andThen(Tuple2.of(e.getKey()))).toMapEntry()
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static <T,K,V> T foldLeft(Map<K,V> m, T t, Function<T,BiFunction<K,V,T>> f) {
        return m.entrySet().stream().reduce(t,
                (acc,e) -> f.apply(acc).apply(e.getKey(), e.getValue()),
                (acc1, acc2) -> acc2
        );
    }

    static <K,V> Map<K,V> merge(Map<K,V> m1, Map<K,V> m2) {
        return merge(m1,m2,(v1,v2) -> v2);
    }

    static <K,V> Map<K,V> merge(Map<K,V> m1, Map<K,V> m2, BinaryOperator<V> f) {
        return Stream.concat(m1.entrySet().stream(), m2.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, f, HashMap::new));
    }

    static <K,V> Map<K,V> mergeEntry(Map<K,V> m, K k, V v) {
        return merge(m, Collections.singletonMap(k,v),(v1,v2)->v2);
    }

    static <K,V> Map<K,V> mergeEntry(Map<K,V> m, K k, V v, BinaryOperator<V> f) {
        return merge(m, Collections.singletonMap(k,v),f);
    }

    static <K,V> ExtendedMap<K, V> extendedMap() {
        return extendedMap(new HashMap<>());
    }

    static <K,V> ExtendedMap<K, V> extendedMap(Map<K,V> m) {
        return new ExtendedMap<>(m);
    }

    class ExtendedMap<K,V> extends HashMap<K,V> implements Maps {
        public ExtendedMap(Map<K, V> m) {
            super(m);
        }

        public <T> ExtendedMap<K,T> map(BiFunction<K,V, T> f) {
            return extendedMap(Maps.map(this, f));
        }

        public ExtendedMap<K,V> merge(Map<K,V> m) {
            return extendedMap(Maps.merge(this, m));
        }

        public ExtendedMap<K,V> merge(Map<K,V> m, BinaryOperator<V> f) {
            return extendedMap(Maps.merge(this,m,f));
        }

        public ExtendedMap<K,V> mergeEntry(K k, V v) {
            return extendedMap(Maps.mergeEntry(this,k,v));
        }

        public ExtendedMap<K,V> mergeEntry(K k, V v, BinaryOperator<V> f) {
            return extendedMap(Maps.mergeEntry(this,k,v,f));
        }

        public <T> T foldLeft(T t, Function<T,BiFunction<K,V,T>> f) {
            return Maps.foldLeft(this, t, f);
        }
    }
}
