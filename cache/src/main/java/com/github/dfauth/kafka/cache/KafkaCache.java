package com.github.dfauth.kafka.cache;

import com.github.dfauth.kafka.RebalanceListener;
import com.github.dfauth.kafka.StreamBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.dfauth.functional.Functions.peek;
import static com.github.dfauth.functional.Try.tryWithCallable;
import static com.github.dfauth.kafka.RebalanceListener.noOp;

public class KafkaCache<K, V, T, R, S> {

    private final StreamBuilder<K, V, T, R> builder;
    private final Cache<T, S> cache;
    private final BiConsumer<T, R> messageConsumer;
    private StreamBuilder.KafkaStream<K, V> stream;
    private BiFunction<S,R,S> computeIfPresent;
    private Function<T,S> computeIfAbsent;

    public KafkaCache(StreamBuilder<K, V, T, R> builder, Cache<T, S> cache, BiConsumer<T, R> messageConsumer, RebalanceListener<K,V> partitionAssignmentConsumer, RebalanceListener<K,V> partitionRevocationConsumer, BiFunction<S,R,S> computeIfPresent, Function<T,S> computeIfAbsent) {
        this.builder = builder;
        this.cache = cache;
        this.messageConsumer = messageConsumer;
        this.builder.onPartitionAssignment(partitionAssignmentConsumer);
        this.builder.onPartitionRevocation(partitionRevocationConsumer);
        this.computeIfPresent = computeIfPresent;
        this.computeIfAbsent = computeIfAbsent;
    }

    public static <K, V, T, R, S> Builder<K, V, T, R, S> builder() {
        return new Builder<>();
    }

    public static Builder<String,String,String,String,String> stringBuilder() {
        return new Builder<String,String,String,String,String>()
                .withKeyMapper((k,v) -> k)
                .withKeyDeserializer(new StringDeserializer())
                .withValueDeserializer(new StringDeserializer())
                .withValueMapper((k,v)->v);
    }

    public static <K, V, R, S> Builder<K, V, K, R, S> unmappedKeyBuilder() {
        return new Builder<K, V, K, R, S>()
                .withKeyMapper((k,v) -> k);
    }

    public static <V, R, S> Builder<String, V, String, R, S> unmappedStringKeyBuilder() {
        return new Builder<String, V, String, R, S>()
                .withKeyMapper((k,v) -> k)
                .withKeyDeserializer(new StringDeserializer());
    }

    public static <V, R, S> Builder<Long, V, Long, R, S> unmappedLongKeyBuilder() {
        return new Builder<Long, V, Long, R, S>()
                .withKeyMapper((k,v) -> k)
                .withKeyDeserializer(new LongDeserializer());
    }

    public static <K, V, T, S> Builder<K, V, T, V, S> unmappedValueBuilder() {
        return new Builder<K, V, T, V, S>()
                .withValueMapper((k,v) -> v);
    }

    public static <K, V, S> Builder<K, V, K, V, S> unmappedBuilder() {
        return new Builder<K, V, K, V, S>()
                .withKeyMapper((k,v) -> k)
                .withValueMapper((k,v) -> v);
    }

    public void start() {
        this.stream = this.builder.withKeyValueConsumer((t, r) -> {
            cache.asMap()
                    .compute(t,
                            (k,v) -> Optional.ofNullable(v)
                                    .map(oldV -> computeIfPresent.apply(oldV, r))
                                    .orElseGet(() -> computeIfPresent.apply(tryWithCallable(() -> computeIfAbsent.apply(t)).toOptional().orElse(null),r))
                    );
            this.messageConsumer.accept(t, r);
        }).build();
        this.stream.start();
    }

    public void stop() {
        this.stream.stop();
    }

    public static class Builder<K, V, T, R, S> {

        private final StreamBuilder<K, V, T, R> streamBuilder = StreamBuilder.builder();
        private BiFunction<K, V, T> keyMapper = (k,v) -> (T)k;
        private BiFunction<K, V, R> valueMapper = (k,v) -> (R)v;
        private RebalanceListener<K,V> partitionAssignmentConsumer = noOp();
        private RebalanceListener<K,V> partitionRevocationConsumer = noOp();
        private BiConsumer<T, R> messageConsumer = (k,v) -> {};
        private Consumer<CacheBuilder<Object, Object>> cacheConfigurer = b -> {};
        private BiFunction<S,R,S> computeIfPresent = (oldR, newR) -> (S)newR;
        private Function<T, S> cacheMiss = t -> {
            throw new IllegalArgumentException("no cache value for key "+t);
        };


        public KafkaCache<K, V, T, R, S> build() {

            CacheLoader<T,S> cacheLoader = new CacheLoader<T, S>() {
                @Override
                public S load(T t) {
                    return cacheMiss.apply(t);
                }
            };
            return new KafkaCache<>(
                    streamBuilder.withKeyMapper(k -> keyMapper.apply(k, null)).withValueMapper(v -> valueMapper.apply(null, v)),
                    peek(cacheConfigurer).apply(CacheBuilder.newBuilder()).build(cacheLoader),
                    messageConsumer,
                    partitionAssignmentConsumer,
                    partitionRevocationConsumer,
                    computeIfPresent,
                    cacheMiss  // computeIfAbsent
            );
        }

        private R create(T t) {
            return null;
        }

        public KafkaCache.Builder<K, V, T, R, S> withKeyDeserializer(Deserializer<K> keyDeserializer) {
            streamBuilder.withKeyDeserializer(keyDeserializer);
            return this;
        }

        public KafkaCache.Builder<K, V, T, R, S> withValueDeserializer(Deserializer<V> valueDeserializer) {
            streamBuilder.withValueDeserializer(valueDeserializer);
            return this;
        }

        @SafeVarargs
        public final Builder<K, V, T, R, S> withProperties(Map<String, Object>... configs) {
            streamBuilder.withProperties(configs);
            return this;
        }

        public Builder<K, V, T, R, S> withTopic(String topic) {
            streamBuilder.withTopic(topic);
            return this;
        }

        public Builder<K, V, T, R, S> withCacheConfiguration(Consumer<CacheBuilder<Object, Object>> cacheConfigurer) {
            this.cacheConfigurer = cacheConfigurer;
            return this;
        }

        public Builder<K, V, T, R, S> onCacheMiss(Function<T,S> cacheMiss) {
            this.cacheMiss = cacheMiss;
            return this;
        }

        public Builder<K, V, T, R, S> withKeyMapper(BiFunction<K,V,T> keyMapper) {
            this.keyMapper = keyMapper;
            return this;
        }

        public Builder<K, V, T, R, S> withValueMapper(BiFunction<K,V,R> valueMapper) {
            this.valueMapper = valueMapper;
            return this;
        }

        public Builder<K, V, T, R, S> onPartitionAssignment(RebalanceListener<K,V> consumer) {
            this.partitionAssignmentConsumer = consumer;
            return this;
        }

        public Builder<K, V, T, R, S> onPartitionRevocation(RebalanceListener<K,V> consumer) {
            this.partitionRevocationConsumer = consumer;
            return this;
        }

        public Builder<K, V, T, R, S> onMessage(BiConsumer<T, R> consumer) {
            this.messageConsumer = consumer;
            return this;
        }

        public Builder<K, V, T, R, S> computeIfPresent(BiFunction<S,R,S> computeIfPresent) {
            this.computeIfPresent = computeIfPresent;
            return this;
        }
    }
}
