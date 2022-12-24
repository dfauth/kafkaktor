package com.github.dfauth.kafka;

import org.apache.kafka.common.TopicPartition;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface RebalanceListener<K,V> extends KafkaConsumerAware<K,V, Consumer<Collection<TopicPartition>>> {

    static <K,V> RebalanceListener<K, V> noOp() {
        return c -> tps -> {};
    }

    static <K,V> RebalanceListener<K,V> seekToBeginning() {
        return c -> c::seekToBeginning;
    }

    static <K,V> RebalanceListener<K,V> seekToEnd() {
        return c -> c::seekToEnd;
    }

    static <K,V> RebalanceListener<K,V> seekToOffset(Map<TopicPartition,Long> m) {
        return c -> tps -> tps.forEach(tp -> Optional.ofNullable(m.get(tp)).ifPresent(_o -> c.seek(tp,_o)));
    }

    static <K,V> RebalanceListener<K,V> seekToTimestamp(Instant i) {
        return seekToTimestamp(_ignored -> i.toEpochMilli());
    }

    static <K,V> RebalanceListener<K,V> seekToTimestamp(Function<TopicPartition,Long> f) {
        return c -> tps -> c.offsetsForTimes(
                tps.stream().map(tp -> Map.entry(tp,f.apply(tp))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        ).forEach((tp,o) -> c.seek(tp,o.offset()));
    }

    default RebalanceListener<K,V> andThen(RebalanceListener<K,V> next) {
        return c -> {
            Consumer<Collection<TopicPartition>> _this = withKafkaConsumer(c);
            Consumer<Collection<TopicPartition>> _next = next.withKafkaConsumer(c);
            return tps -> {
                _this.accept(tps);
                _next.accept(tps);
            };
        };
    }

    default RebalanceListener<K,V> compose(RebalanceListener<K,V> next) {
        return next.andThen(this);
    }
}
