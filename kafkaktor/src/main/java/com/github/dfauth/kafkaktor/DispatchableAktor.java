package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.Dispatchable;
import com.github.dfauth.avro.Envelope;
import com.github.dfauth.avro.EnvelopeHandler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class DispatchableAktor implements Aktor<Dispatchable>, EnvelopeHandler<AktorMessageContext> {

    private final KafkaAktorContext ctx;

    protected DispatchableAktor(KafkaAktorContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public MessageContextAware<Consumer<Dispatchable>> withAktorContext(AktorContext ktx) {
        return m -> p -> {
            p.dispatch(Envelope.asEnvelope(m, p), this);
        };
    }

    @Override
    public CompletableFuture<AktorAddress> start() {
        return ctx.address();
    }
}
