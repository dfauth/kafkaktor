package com.github.dfauth.kafkaktor;

import org.apache.avro.specific.SpecificRecord;

import java.util.function.Consumer;

public interface Aktor<T extends SpecificRecord> extends AktorContextAware<MessageContextAware<Consumer<T>>>, AktorLifecycle<T> {

    static <T extends SpecificRecord> AktorContextAware<MessageContextAware<Consumer<T>>> onStartup(Consumer<AktorContext> c) {
        return ctx -> {
            c.accept(ctx);
            return m -> p -> {};
        };
    }

}
