package com.github.dfauth.functional;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public interface Reducer<T,U> {

    U identity();

    default BiFunction<U,T,U> accumulator() {
        return (u,t) -> u;
    }

    default boolean isParallel() {
        return false;
    }

    default BinaryOperator<U> combiner() {
        return (u1,u2) -> {
            throw new UnsupportedOperationException("Oops. Should never happen");
        };
    }

    default U reduce(Collection<T> c) {
        return reduce(this, c);
    }

    static <T,U> U reduce(Reducer<T,U> r, Collection<T> l) {
        return Optional.ofNullable(l).filter(_ignored -> r.isParallel()).map(_l ->
        _l.parallelStream().reduce(r.identity(), r.accumulator(), r.combiner())).orElseGet(() ->
                        l.stream().reduce(r.identity(), r.accumulator(), r.combiner())
                );
    }

    static <K,V> Reducer<Map.Entry<K,V>, Map<K, List<V>>> mapEntryGroupingMappingReducer() {
        return groupingMappingReducer(Map.Entry::getKey, Map.Entry::getValue);
    }

    static <T,K,V> Reducer<T, Map<K, List<V>>> groupingMappingReducer(Function<T,K> classifier, Function<T,V> valueMapper) {
        return new Reducer<>() {
            @Override
            public Map<K, List<V>> identity() {
                return new HashMap<>();
            }

            @Override
            public BiFunction<Map<K, List<V>>, T, Map<K, List<V>>> accumulator() {
                return (acc, t) -> {
                    acc.compute(classifier.apply(t),
                            (k, v) -> Optional.ofNullable(v).map(l -> {
                                l.add(valueMapper.apply(t));
                                return l;
                            }).orElseGet(() -> {
                                List<V> tmp = new ArrayList<>();
                                tmp.add(valueMapper.apply(t));
                                return tmp;
                            })
                    );
                    return acc;
                };
            }
        };
    }

}
