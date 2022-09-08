package com.github.dfauth.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import java.util.function.Consumer;
import java.util.function.Function;

public interface OffsetContextAware<T> {

    T withOffsetContext(Function<TopicPartition, Offsets> f);

    class OffsetContextAwareRecordConsumer<K,V> implements OffsetContextAware<Consumer<ConsumerRecord<K,V>>> {

        private final Function<Offsets, Consumer<ConsumerRecord<K,V>>> nested;

        public OffsetContextAwareRecordConsumer(Function<Offsets, Consumer<ConsumerRecord<K, V>>> nested) {
            this.nested = nested;
        }

        public static <K,V> OffsetContextAwareRecordConsumer<K,V> ignoreOffsetContext(Consumer<ConsumerRecord<K,V>> consumer) {
            return new OffsetContextAwareRecordConsumer<>(ignored -> consumer);
        }

        public Consumer<ConsumerRecord<K,V>> withOffsetContext(Function<TopicPartition, Offsets> f) {
            return r -> {
                Offsets o = f.apply(new TopicPartition(r.topic(), r.partition()));
                nested.apply(o).accept(r);
            };
        }

    }
}
