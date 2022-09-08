package com.github.dfauth.functional;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Maps {

    public static <K,V,T> Function<Map<K,V>,Map<K,T>> mapTransformerOf(BiFunction<K,V,T> f) {
        return m -> m.entrySet().stream().map(mapEntryToTuple2Transformer(f)).collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));
    }

    public static <T, V, K> Function<Map.Entry<K,V>, Tuple2<K,T>> mapEntryToTuple2Transformer(BiFunction<K,V,T> f) {
        return mapEntryTransformer((k,v) -> Tuple2.of(k, f.apply(k,v)));
    }

    public static <T, V, K> Function<Map.Entry<K,V>, T> mapEntryTransformer(BiFunction<K,V,T> f) {
        return e -> f.apply(e.getKey(), e.getValue());
    }

}
