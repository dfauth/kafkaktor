package com.github.dfauth.functional;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Maps {

    public static <K,V,T> Function<Map<K,V>,Map<K,T>> mapTransformerOf(BiFunction<K,V,T> f) {
        return mapTransformerOf(e -> Tuple2.of(e.getKey(), f.apply(e.getKey(), e.getValue())).toMapEntry());
    }

    public static <K,V,T,R> Function<Map<K,V>,Map<T,R>> mapTransformerOf(Function<Map.Entry<K, V>, Map.Entry<T,R>> f) {
        return m -> m.entrySet().stream().map(f).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <T, V, K> Function<Map.Entry<K,V>, T> mapEntryTransformer(BiFunction<K,V,T> f) {
        return e -> f.apply(e.getKey(), e.getValue());
    }

}
