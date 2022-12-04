package com.github.dfauth.kafkaktor;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class LambdaAktor<T extends SpecificRecord> extends AktorBase<T> implements Aktor<T> {

    private final AktorContextAware<MessageContextAware<Consumer<T>>> lambda;

    public LambdaAktor(AktorContext ctx, AktorContextAware<MessageContextAware<Consumer<T>>> lambda) {
        super(ctx);
        this.lambda = lambda;
    }

    @Override
    public MessageContextAware<Consumer<T>> withAktorContext(AktorContext ktx) {
        MessageContextAware<Consumer<T>> x = lambda.withAktorContext(ktx);
        return m -> p -> x.withMessageContext(m).accept(p);
    }

    @Override
    public CompletableFuture<AktorAddress> start() {
        return null;
    }
}
