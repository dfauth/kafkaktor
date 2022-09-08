package com.github.dfauth.kafkaktor;

import org.apache.avro.specific.SpecificRecord;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AktorContext {

    String key();

    default <R extends SpecificRecord> CompletableFuture<AktorReference<R>> spawn(Class<? extends Aktor<R>> aktorClass) {
        return spawn(UUID.randomUUID().toString(), aktorClass);
    }

    <R extends SpecificRecord> CompletableFuture<AktorReference<R>> spawn(String key, Class<? extends Aktor<R>> aktorClass);
}
