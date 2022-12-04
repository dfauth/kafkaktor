package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.Dispatchable;
import com.github.dfauth.avro.Envelope;
import com.github.dfauth.avro.EnvelopeHandler;
import com.github.dfauth.avro.actor.ActorCreationRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class Supervisor<T extends Dispatchable<T>> extends AktorBase<T> implements Aktor<T>, EnvelopeHandler<AktorMessageContext> {

    public static final String TOPIC = "messages";
    public static final String KEY = "supervisor";

    private final Map<String, MessageContextAware<Consumer<SpecificRecord>>> aktors = new HashMap<>();

    public Supervisor(KafkaContext ctx) {
        super(ctx);
    }

    public static AktorReference<SpecificRecord> create(AktorSystem system) {
        return create(system, Supervisor.TOPIC);
    }

    public static AktorReference<SpecificRecord> create(AktorSystem system, String topic) {
        return system.newAktor("supervisor", topic, Supervisor.class);
    }

    @Override
    public MessageContextAware<Consumer<T>> withAktorContext(AktorContext ktx) {
        return m -> p -> p.dispatch(Envelope.asEnvelope(m,p), this);
    }

    @Override
    public void handleActorCreationRequest(Envelope<ActorCreationRequest, AktorMessageContext> e) {
    }

    @Override
    public <R extends Dispatchable<R>> void handleOther(Envelope<R, AktorMessageContext> e) {
        String key = e.messageContext().key();
        aktors.get(key).withMessageContext(e.messageContext()).accept(e.payload());
    }

    @Override
    public CompletableFuture<AktorAddress> start() {
        return null;
    }
}
