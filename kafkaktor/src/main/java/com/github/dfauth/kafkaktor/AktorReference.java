package com.github.dfauth.kafkaktor;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

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

    CompletableFuture<RecordMetadata> tell(T t, Map<String, Object> h);

    String key();
}
