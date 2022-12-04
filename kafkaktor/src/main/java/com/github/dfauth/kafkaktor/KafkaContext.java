package com.github.dfauth.kafkaktor;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class KafkaContext implements AktorContext {

    private final AktorSystem ctx;

    public KafkaContext(AktorSystem ctx) {
        this.ctx = ctx;
    }

    @Override
    public <R extends SpecificRecord> CompletableFuture<AktorReference<R>> spawn(Class<? extends Aktor<R>> aktorClass) {
        return AktorContext.super.spawn(aktorClass);
    }

    @Override
    public <R extends SpecificRecord> CompletableFuture<AktorReference<R>> spawn(String key, Class<? extends Aktor<R>> aktorClass) {
        return null;
    }

    public <T extends SpecificRecord> CompletableFuture<RecordMetadata> tell(String key, T t, Map<String, Object> m) {
        return ctx.sink().publish(key, t,m);
    }
}
