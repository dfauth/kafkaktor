package com.github.dfauth.kafka;

import com.github.dfauth.kafka.utils.PrimitiveHeader;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.unmodifiableMap;

public class KafkaSink<K,V> {

    private final KafkaProducer<K, V> producer;
    private final String topic;
    private final BiFunction<K, V, Integer> partitioner;

    public KafkaSink(Map<String, Object> props, String topic, Serializer<K> keySerializer, Serializer<V> valueSerializer, BiFunction<K,V,Integer> partitioner) {
        this.producer = new KafkaProducer<>(props, keySerializer, valueSerializer);
        this.topic = topic;
        this.partitioner = partitioner;
    }

    public static <K,V> Builder<K,V> newBuilder() {
        return new Builder<>();
    }

    public static <V> Builder<String, V> newStringKeyBuilder() {
        return KafkaSink.<String,V>newBuilder().withKeySerializer(new StringSerializer());
    }

    public static <V> Builder<Long, V> newLongKeyBuilder() {
        return KafkaSink.<Long,V>newBuilder().withKeySerializer(new LongSerializer());
    }

    public static <K> Builder<K, String> newStringValueBuilder() {
        return KafkaSink.<K, String>newBuilder().withValueSerializer(new StringSerializer());
    }

    public static Builder<String, String> newStringBuilder() {
        return KafkaSink.<String>newStringKeyBuilder().withValueSerializer(new StringSerializer());
    }

    public CompletableFuture<RecordMetadata> publish(K k, V v) {
        return publish(topic, k, v, Collections.emptyMap());
    }

    public CompletableFuture<RecordMetadata> publish(String topic, K k, V v) {
        return publish(topic, k, v, Collections.emptyMap());
    }

    public CompletableFuture<RecordMetadata> publish(K k, V v, Map<String, Object> metadata) {
        return publish(topic, k, v, metadata);
    }

    public CompletableFuture<RecordMetadata> publish(String topic, K k, V v, Map<String, Object> metadata) {
        return publish(topic, partitioner.apply(k,v), k, v, metadata);
    }

    public CompletableFuture<RecordMetadata> publish(String topic, Integer partition, K k, V v, Map<String, Object> metadata) {
        CompletableFuture<RecordMetadata> f = new CompletableFuture<>();
        producer.send(new ProducerRecord<>(topic, partition, k, v, PrimitiveHeader.create(metadata)), (m, e) -> {
            if(e != null) {
                f.completeExceptionally(e);
            } else {
                f.complete(m);
            }
        });
        return f;
    }

    public static class Builder<K,V> {

        private Serializer<K> keySerializer;
        private Serializer<V> valueSerializer;
        private Map<String, Object> props;
        private String topic;
        private BiFunction<K,V,Integer> partitioner = (k,v) -> null;

        public KafkaSink<K,V> build() {
            return new KafkaSink<>(props, topic, keySerializer, valueSerializer, partitioner);
        }

        public Builder<K, V> withKeySerializer(Serializer<K> serializer) {
            this.keySerializer = serializer;
            return this;
        }

        public Builder<K, V> withValueSerializer(Serializer<V> serializer) {
            this.valueSerializer = serializer;
            return this;
        }

        public KafkaSink.Builder<K,V> withProperties(Map<String, Object> props) {
            this.props = unmodifiableMap(props);
            return this;
        }

        public KafkaSink.Builder<K,V> withProperties(Map<String, Object> props, String key, String value) {
            Map<String, Object> tmp = new HashMap<>(props);
            tmp.put(key, value);
            this.props = unmodifiableMap(tmp);
            return this;
        }

        public KafkaSink.Builder<K,V> withTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public KafkaSink.Builder<K,V> withPartitioner(Function<K, Integer> partitioner) {
            return withPartitioner((k,v) -> partitioner.apply(k));
        }

        public KafkaSink.Builder<K,V> withPartitioner(BiFunction<K, V, Integer> partitioner) {
            this.partitioner = partitioner;
            return this;
        }
    }
}
