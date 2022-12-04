package com.github.dfauth.kafkaktor;

import com.github.dfauth.functional.Maps;
import com.github.dfauth.kafka.KafkaSink;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.github.dfauth.kafkaktor.AktorMessageContext.SENDER_KEY;

@Slf4j
public class KafkaAktorReference<T extends SpecificRecord> implements AktorReference<T> {

    private final KafkaContext ktx;
    private final String key;

    public KafkaAktorReference(String key, KafkaContext ktx) {
        this.key = key;
        this.ktx = ktx;
    }

    @Override
    public <R extends SpecificRecord> CompletableFuture<R> ask(T t, Map<String, Object> m) {
        return null;
    }

    private void withSink(Consumer<KafkaSink<String,T>> consumer) {
    }

    @Override
    public CompletableFuture<RecordMetadata> tell(T t, Map<String, Object> m) {
        return ktx.tell(this.key,t, Maps.mergeEntry(m,SENDER_KEY,key()));
    }

    @Override
    public String key() {
        return key;
    }
}
