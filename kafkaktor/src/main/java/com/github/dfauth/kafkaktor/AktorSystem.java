package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.KafkaAvroSerde;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;

public class AktorSystem {

    protected final Map<String, Object> config;
    private final KafkaAvroSerde serde;

    public AktorSystem(Map<String, Object> config, KafkaAvroSerde serde) {
        this.config = config;
        this.serde = serde;
    }

    public static AktorSystem create(Map<String, Object> config, KafkaAvroSerde serde) {
        return new AktorSystem(config, serde);
    }

    public <T extends SpecificRecord> AktorReference<T> newAktor(String key, Class<?> aktorClass) {
        return newAktor(key, key, aktorClass);
    }

    public <T extends SpecificRecord> AktorReference<T> newAktor(String key, String topic, Class<?> aktorClass) {
        KafkaContext ctx = new KafkaContext(topic, this);
        Aktor<T> aktor = Aktor.class.cast(tryCatch(() ->
            aktorClass.getDeclaredConstructor(new Class[]{KafkaAktorContext.class}).newInstance(new Object[]{new KafkaAktorContext(key, ctx)})
        ));
        aktor.start();
        return null;
    }

    public <T extends SpecificRecord> CompletableFuture<AktorReference<T>> newAktor(AktorContextAware<MessageContextAware<Consumer<T>>> c) {
        return _newAktor(c).thenApply(ref -> ref);
    }

    public <T extends SpecificRecord> CompletableFuture<KafkaAktorReference<T>> _newAktor(AktorContextAware<MessageContextAware<Consumer<T>>> c) {
        String key = UUID.randomUUID().toString();
        String topic = "temp";
        KafkaAktorContext ktx = new KafkaAktorContext(key, new KafkaContext(topic, this));
        LambdaAktor<T> lambda = new LambdaAktor<>(ktx, c);
        return lambda.start().thenApply(a -> new KafkaAktorReference<>(ktx, a));
    }

    public AktorContext contextFor(String key, String topic) {
        return new KafkaAktorContext(key, new KafkaContext(topic, this));
    }

    public <T extends SpecificRecord> Serializer<T> serializer() {
        try(Serde<T> s = serde.serde()) {
            return s.serializer();
        }
    }

    public <T extends SpecificRecord> Deserializer<T> deserializer() {
        try(Serde<T> s = serde.serde()) {
            return s.deserializer();
        }
    }
}
