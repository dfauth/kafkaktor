package com.github.dfauth.kafkaktor;

import org.apache.avro.specific.SpecificRecord;

import java.util.concurrent.CompletableFuture;

public class KafkaAktorContext implements AktorContext {

    private final String key;
    private final KafkaContext ktx;

    public KafkaAktorContext(String key, KafkaContext ktx) {
        this.key = key;
        this.ktx = ktx;
    }

    public KafkaContext kafkaContext() {
        return ktx;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public <R extends SpecificRecord> CompletableFuture<AktorReference<R>> spawn(String key, Class<? extends Aktor<R>> aktorClass) {
        return ktx.spawn(key, aktorClass);
    }

    public CompletableFuture<AktorAddress> address() {
        CompletableFuture<AktorAddress> f = new CompletableFuture<>();
        ktx.currentState().payload().ifPresent(
                tps -> f.complete(new AktorAddress(key, ktx.topic(), tps.iterator().next().partition())) // TODO
        );
        return f;
    }

    public KafkaAktorContext create(String key) {
        return new KafkaAktorContext(key,ktx);
    }
}
