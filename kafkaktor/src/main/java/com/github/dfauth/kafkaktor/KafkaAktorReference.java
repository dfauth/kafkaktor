package com.github.dfauth.kafkaktor;

import com.github.dfauth.kafka.KafkaSink;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class KafkaAktorReference<T extends SpecificRecord> implements AktorReference<T> {

    private final KafkaAktorContext ktx;
    private final AktorAddress address;

    public KafkaAktorReference(KafkaAktorContext ktx, AktorAddress address) {
        this.ktx = ktx;
        this.address = address;
    }

    @Override
    public <R extends SpecificRecord> CompletableFuture<R> ask(T t, Map<String, Object> m) {
        CompletableFuture<R> f = new CompletableFuture<>();
        CompletableFuture<KafkaAktorReference<R>> fRef = ktx.kafkaContext().aktorSystem()._newAktor(_ktx -> mctx -> f::complete);
        fRef.thenAccept(ref ->
                withSink(s ->
                        ktx.address().thenAccept(a -> address.publish(s,t,a.metadata(m)))));
        return f;
    }

    private void withSink(Consumer<KafkaSink<String,T>> consumer) {
        AktorSystem system = ktx.kafkaContext().aktorSystem();
        KafkaSink<String, T> sink = KafkaSink.<T>newStringKeyBuilder()
                .withTopic(address.topic())
                .withProperties(system.config)
                .withValueSerializer(system.serializer()).build();
        consumer.accept(sink);
    }

    @Override
    public void tell(T t, Map<String, Object> m) {
        withSink(s -> ktx.address().thenAccept(a -> address.publish(s, t, a.metadata(m))));
    }

    @Override
    public String key() {
        return ktx.key();
    }
}
