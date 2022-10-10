package com.github.dfauth.functional;

import java.util.Map;
import java.util.stream.Collector;

public interface Collectors {

    static <K,V> Collector<Map.Entry<K,V>, ?, Map<K, V>> mapEntryCollector() {
        return java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}
