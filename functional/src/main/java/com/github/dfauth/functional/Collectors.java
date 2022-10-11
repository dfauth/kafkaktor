package com.github.dfauth.functional;

import java.util.Map;
import java.util.stream.Collector;

public interface Collectors {

    static <K,V> Collector<Map.Entry<K,V>, ?, Map<K, V>> mapEntryCollector() {
        return java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }
    static <K,V> Collector<Tuple2<K,V>, ?, Map<K, V>> tuple2Collector() {
        return java.util.stream.Collectors.toMap(Tuple2::_1, Tuple2::_2);
    }
}
