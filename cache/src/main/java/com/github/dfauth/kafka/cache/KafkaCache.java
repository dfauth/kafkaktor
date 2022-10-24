package com.github.dfauth.kafka.cache;

import com.github.dfauth.kafka.RebalanceListener;
import com.github.dfauth.kafka.StreamBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.github.dfauth.kafka.RebalanceListener.noOp;

public class KafkaCache<K, V, T, R> {

    private final StreamBuilder<K, V, T, R> builder;
    private final Cache<T, R> cache;
    private final BiConsumer<T, R> messageConsumer;
    private StreamBuilder.KafkaStream<K, V> stream;

    public KafkaCache(StreamBuilder<K, V, T, R> builder, Cache<T, R> cache, BiConsumer<T, R> messageConsumer, RebalanceListener<K,V> partitionAssignmentConsumer, RebalanceListener<K,V> partitionRevocationConsumer) {
        this.builder = builder;
        this.cache = cache;
        this.messageConsumer = messageConsumer;
        this.builder.onPartitionAssignment(partitionAssignmentConsumer);
        this.builder.onPartitionRevocation(partitionRevocationConsumer);
    }

    public static <K, V, T, R> Builder<K, V, T, R> builder() {
        return new Builder<>();
    }

    public static Builder<String,String,String,String> stringBuilder() {
        return new Builder<String,String,String,String>()
                .withKeyMapper((k,v) -> k)
                .withKeyDeserializer(new StringDeserializer())
                .withValueDeserializer(new StringDeserializer())
                .withValueMapper((k,v)->v);
    }

    public static <K, V, R> Builder<K, V, K, R> unmappedKeyBuilder() {
        return new Builder<K, V, K, R>()
                .withKeyMapper((k,v) -> k);
    }

    public static <V, R> Builder<String, V, String, R> unmappedStringKeyBuilder() {
        return new Builder<String, V, String, R>()
                .withKeyMapper((k,v) -> k)
                .withKeyDeserializer(new StringDeserializer());
    }

    public static <V, R> Builder<Long, V, Long, R> unmappedLongKeyBuilder() {
        return new Builder<Long, V, Long, R>()
                .withKeyMapper((k,v) -> k)
                .withKeyDeserializer(new LongDeserializer());
    }

    public static <K, V, T> Builder<K, V, T, V> unmappedValueBuilder() {
        return new Builder<K, V, T, V>()
                .withValueMapper((k,v) -> v);
    }

    public static <K, V> Builder<K, V, K, V> unmappedBuilder() {
        return new Builder<K, V, K, V>()
                .withKeyMapper((k,v) -> k)
                .withValueMapper((k,v) -> v);
    }

    public void start() {
        this.stream = this.builder.withKeyValueConsumer((k, v) -> {
            cache.put(k, v);
            this.messageConsumer.accept(k, cache.getIfPresent(k));
        }).build();
        this.stream.start();
    }

    public void stop() {
        this.stream.stop();
    }

    public Optional<R> getOptional(T t) {
        return Optional.ofNullable(cache.getIfPresent(t));
    }

    public static class Builder<K, V, T, R> {

        private final StreamBuilder<K, V, T, R> streamBuilder = StreamBuilder.builder();
        private final CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        private BiFunction<K, V, T> keyMapper = (k,v) -> (T)k;
        private BiFunction<K, V, R> valueMapper = (k,v) -> (R)v;
        private RebalanceListener<K,V> partitionAssignmentConsumer = noOp();
        private RebalanceListener<K,V> partitionRevocationConsumer = noOp();
        private BiConsumer<T, R> messageConsumer = (k,v) -> {};

        public KafkaCache<K, V, T, R> build() {

            return new KafkaCache<>(
                    streamBuilder.withKeyMapper(k -> keyMapper.apply(k, null)).withValueMapper(v -> valueMapper.apply(null, v)),
                    cacheBuilder.build(),
                    messageConsumer,
                    partitionAssignmentConsumer,
                    partitionRevocationConsumer
            );
        }

        public KafkaCache.Builder<K, V, T, R> withKeyDeserializer(Deserializer<K> keyDeserializer) {
            streamBuilder.withKeyDeserializer(keyDeserializer);
            return this;
        }

        public KafkaCache.Builder<K, V, T, R> withValueDeserializer(Deserializer<V> valueDeserializer) {
            streamBuilder.withValueDeserializer(valueDeserializer);
            return this;
        }

        @SafeVarargs
        public final Builder<K, V, T, R> withProperties(Map<String, Object>... configs) {
            streamBuilder.withProperties(configs);
            return this;
        }

        public Builder<K, V, T, R> withTopic(String topic) {
            streamBuilder.withTopic(topic);
            return this;
        }

        public Builder<K, V, T, R> withCacheConfiguration(Consumer<CacheBuilder<Object, Object>> consumer) {
            consumer.accept(cacheBuilder);
            return this;
        }

        public Builder<K, V, T, R> withKeyMapper(BiFunction<K,V,T> keyMapper) {
            this.keyMapper = keyMapper;
            return this;
        }

        public Builder<K, V, T, R> withValueMapper(BiFunction<K,V,R> valueMapper) {
            this.valueMapper = valueMapper;
            return this;
        }

        public Builder<K, V, T, R> onPartitionAssignment(RebalanceListener<K,V> consumer) {
            this.partitionAssignmentConsumer = consumer;
            return this;
        }

        public Builder<K, V, T, R> onPartitionRevocation(RebalanceListener<K,V> consumer) {
            this.partitionRevocationConsumer = consumer;
            return this;
        }

        public Builder<K, V, T, R> onMessage(BiConsumer<T, R> consumer) {
            this.messageConsumer = consumer;
            return this;
        }
    }
}
