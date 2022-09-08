package com.github.dfauth.kafkaktor;

import org.apache.avro.specific.SpecificRecord;

import java.util.concurrent.CompletableFuture;

public interface AktorLifecycle<T extends SpecificRecord> {

    CompletableFuture<AktorAddress> start();

    default void stop() {}
}
