package com.github.dfauth.kafka;

import com.github.dfauth.functional.Maps;
import org.apache.kafka.common.TopicPartition;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface RebalanceProcessor<K,V> extends RebalanceFunction<K,V,Collection<TopicPartition>,Map<TopicPartition,Long>> {

    static <K,V> RebalanceProcessor<K,V> currentOffsets() {
        return currentOffsets(RebalanceListener.noOp());
    }

    static <K,V> RebalanceProcessor<K,V> endOffsets() {
        return endOffsets(RebalanceListener.noOp());
    }

    static <K,V> RebalanceProcessor<K,V> currentOffsets(RebalanceListener<K,V> consumer) {
        return c -> compose(consumer, tps -> Maps.generate(tps, c::position)).withKafkaConsumer(c);
    }

    static <K,V> RebalanceProcessor<K,V> endOffsets(RebalanceListener<K,V> consumer) {
        return c -> compose(consumer, c::endOffsets).withKafkaConsumer(c);
    }

    static <K,V> RebalanceProcessor<K,V> seekToBeginning() {
        return compose(RebalanceListener.seekToBeginning());
    }

    static <K,V> RebalanceProcessor<K,V> seekToEnd() {
        return compose(RebalanceListener.seekToEnd());
    }

    static <K,V> RebalanceProcessor<K,V> seekToTimestamp(Instant i) {
        return compose(RebalanceListener.seekToTimestamp(i));
    }

    static <K,V> RebalanceProcessor<K,V> seekToTimestamp(Function<TopicPartition, Long> f) {
        return compose(RebalanceListener.seekToTimestamp(f));
    }

    static <K,V> RebalanceProcessor<K,V> seekToOffset(Map<TopicPartition, Long> m) {
        return compose(RebalanceListener.seekToOffset(m));
    }

    static <K,V> RebalanceProcessor<K,V> compose(RebalanceListener<K, V> consumer) {
        return c -> compose(consumer, tps -> Maps.generate(tps, c::position)).withKafkaConsumer(c);
    }

    static <K,V> RebalanceProcessor<K,V> compose(RebalanceListener<K, V> consumer, Function<Collection<TopicPartition>, Map<TopicPartition,Long>> f2) {
        return c -> {
            Consumer<Collection<TopicPartition>> _consumer = consumer.withKafkaConsumer(c);
            return tps -> {
                _consumer.accept(tps);
                return f2.apply(tps);
            };
        };
    }
}
