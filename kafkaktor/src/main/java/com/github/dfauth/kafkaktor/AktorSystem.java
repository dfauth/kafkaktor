package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.KafkaAvroSerde;
import com.github.dfauth.kafka.KafkaSink;
import com.github.dfauth.kafka.StreamBuilder;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.github.dfauth.functional.Function1.function1;
import static com.github.dfauth.functional.Functions.peek;
import static com.github.dfauth.kafka.RebalanceListener.seekToBeginning;
import static com.github.dfauth.kafka.utils.PrimitiveHeader.fromHeaders;
import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;

@Slf4j
public class AktorSystem implements AktorContext {

    private String topic;
    private String groupId;
    private StreamBuilder.KafkaStream<String, SpecificRecord> stream;
    private Consumer<StreamBuilder<String,SpecificRecord,String,SpecificRecord>> customizer = b -> {};

    protected final Map<String, Object> config;
    private final KafkaAvroSerde serde;
    private Map<String, Aktor<? extends SpecificRecord>> aktors = new HashMap<>();

    @Builder(setterPrefix = "with", builderClassName = "AktorSystemBuilder")
    public AktorSystem(Map<String, Object> config, String groupId, String topic, KafkaAvroSerde serde, Consumer<StreamBuilder<String,SpecificRecord,String,SpecificRecord>> customizer) {
        this.config = config;
        this.groupId = groupId;
        this.topic = topic;
        this.serde = serde;
        this.customizer = customizer;
    }

    public <T extends SpecificRecord> AktorReference<T> newAktor(String key, String topic, Class<?> aktorClass) {
        Aktor<T> aktor = Aktor.class.cast(tryCatch(() ->
            aktorClass.getDeclaredConstructor(new Class[]{KafkaAktorContext.class}).newInstance(new Object[]{this})
        ));
        return newAktor(key, this, aktor);
    }

    public <T extends SpecificRecord> AktorReference<T> newAktor(String key, AktorContext ctx, Aktor<T> aktor) {
        aktors.put(key, aktor);
        return new KafkaAktorReference<T>(key, new KafkaContext(this));
    }

    public <T extends SpecificRecord> AktorReference<T> newAktor(AktorContextAware<MessageContextAware<Consumer<T>>> c) {
        String key = UUID.randomUUID().toString();
        Aktor<T> lambda = new LambdaAktor<>(this, c);
        return newAktor(key, this, lambda);
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

    public void start(CompletableFuture<?> f) {
        f.exceptionally(function1(this::stop));
        start();
    }

    public void start() {
        this.stream = customize(StreamBuilder.<SpecificRecord>stringKeyUnmappedValueBuilder()
                .withProperties(this.config, ConsumerConfig.GROUP_ID_CONFIG, groupId)
                .withTopic(topic)
                .withValueDeserializer(deserializer()))
                .withRecordConsumer(r -> {
                    aktors.computeIfPresent(r.key(),(k1,v1) -> process(r).apply(k1, (Aktor<SpecificRecord>) v1));
                    aktors.computeIfAbsent(r.key(),k1 -> new Aktor<>() {
                        @Override
                        public MessageContextAware<Consumer<SpecificRecord>> withAktorContext(AktorContext ktx) {
                            return a ->
                                    b -> {
                                        log.info("WOOZ");
                                    };
                        }

                        @Override
                        public CompletableFuture<AktorAddress> start() {
                            return null;
                        }
                    });
                })
                .onPartitionAssignment(seekToBeginning())
                .build();
        this.stream.start();
    }

    private <K,T extends SpecificRecord> BiFunction<String, Aktor<T>, Aktor<T>> process(ConsumerRecord<K,T> r) {
        return (k,a) -> {
            AktorMessageContext m = new AktorMessageContext() {
                @Override
                public String key() {
                    return k;
                }

                @Override
                public Map<String, Object> metadata() {
                    return fromHeaders(r.headers());
                }

                @Override
                public <R extends SpecificRecord> AktorReference<R> sender() {
                    return new KafkaAktorReference<>(metadata().get(SENDER_KEY).toString(), new KafkaContext(AktorSystem.this));
                }
            };
            a.withAktorContext(this).withMessageContext(m).accept(r.value());
            return a;
        };
    }

    private StreamBuilder<String, SpecificRecord, String, SpecificRecord> customize(StreamBuilder<String, SpecificRecord, String, SpecificRecord> streamBuilder) {
        return peek(customizer).apply(streamBuilder);
    }

    public void stop() {
        Optional.ofNullable(this.stream).filter(StreamBuilder.KafkaStream::isStarted).ifPresent(StreamBuilder.KafkaStream::stop);
    }

    @Override
    public <R extends SpecificRecord> CompletableFuture<AktorReference<R>> spawn(Class<? extends Aktor<R>> aktorClass) {
        return null;
    }

    @Override
    public <R extends SpecificRecord> CompletableFuture<AktorReference<R>> spawn(String key, Class<? extends Aktor<R>> aktorClass) {
        return null;
    }

    public KafkaSink<String, SpecificRecord> sink() {
        return KafkaSink.<String, SpecificRecord>newBuilder()
                        .withProperties(this.config)
                                .withTopic(this.topic)
                                        .withKeySerializer(new StringSerializer())
                                                .withValueSerializer(serializer())
                                                        .build();
    }

    static class AktorSystemBuilder {
        private String topic = AktorSystem.class.getCanonicalName();
        private String groupId = AktorSystem.class.getCanonicalName();
    }
}
