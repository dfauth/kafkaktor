package com.github.dfauth.functional;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.dfauth.functional.Tuple2.adapt;
import static com.github.dfauth.functional.Tuple2.asMapEntry;

public class Maps {

    public static <K,V,T> Function<Map<K,V>,Map<K,T>> mapTransformerOf(BiFunction<K,V,T> f) {
        return m -> m.entrySet().stream().map(adapt(asMapEntry(f))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <T, V, K> Function<Map.Entry<K,V>, T> mapEntryTransformer(BiFunction<K,V,T> f) {
        return e -> f.apply(e.getKey(), e.getValue());
    }

}
