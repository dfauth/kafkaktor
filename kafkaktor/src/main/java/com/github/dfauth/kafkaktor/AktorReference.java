package com.github.dfauth.kafkaktor;

import org.apache.avro.specific.SpecificRecord;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AktorReference<T> {

    default <R extends SpecificRecord> CompletableFuture<R> ask(T t) {
        return ask(t, Collections.emptyMap());
    }

    <R extends SpecificRecord> CompletableFuture<R> ask(T t, Map<String, Object> m);

    default void tell(T t) {
        tell(t, Collections.emptyMap());
    }

    void tell(T t, Map<String, Object> h);

    String key();
}
