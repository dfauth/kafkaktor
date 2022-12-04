package com.github.dfauth.kafka;

import com.github.dfauth.functional.Maps;
import org.apache.kafka.common.TopicPartition;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.dfauth.functional.Tuple2.asMapEntry;
import static com.github.dfauth.functional.Tuple2.tuplize;
import static com.github.dfauth.trycatch.TryCatch.Builder.tryCatch;

public interface RebalanceListener<K,V> extends KafkaConsumerAware<Consumer<Collection<TopicPartition>>, K,V>{

    static <K,V> RebalanceListener<K,V> currentOffsets(Consumer<Map<TopicPartition,Long>> consumer) {
        return c -> tps ->
            consumer.accept(Maps.generate(tps, c::position));
    }

    static <K,V> RebalanceListener<K,V> currentOffsets(CompletableFuture<Map<TopicPartition,Long>> f) {
        return c -> tps -> f.complete(Maps.generate(tps, c::position));
    }

    static <K,V> RebalanceListener<K,V> seekToBeginning() {
        return c -> c::seekToBeginning;
    }

    static <K,V> RebalanceListener<K,V> seekToEnd() {
        return c -> c::seekToEnd;
    }

    static <K,V> RebalanceListener<K,V> seekToTimestamp(ZonedDateTime t) {
        return c -> tps ->
                RebalanceListener.<K,V>seekToTimestamp(_ignored -> t).withKafkaConsumer(c).accept(tps);
    }

    static <K,V> RebalanceListener<K,V> seekToTimestamp(Function<TopicPartition, ZonedDateTime> f) {
        return c -> tps ->
                c.offsetsForTimes(tps.stream().map(asMapEntry(f)).collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toInstant().toEpochMilli())))
                        .forEach((tp,o) -> Optional.ofNullable(o).map(_o -> o.offset()).ifPresent(_o -> c.seek(tp,_o)));
    }

    static <K,V> RebalanceListener<K,V> seekToOffsets(Function<TopicPartition, Long> f) {
        return c -> tps -> tps.stream().map(tuplize(f)).forEach(t -> c.seek(t._1(), t._2()));
    }

    static <K,V> RebalanceListener<K, V> noOp() {
        return c -> tps -> {};
    }

    default RebalanceListener<K,V> compose(RebalanceListener<K,V> nested) {
        return c -> tps -> tryCatch(() -> nested.withKafkaConsumer(c).accept(tps))
        .ignoreSilently()
        .andFinallyRun(() -> withKafkaConsumer(c).accept(tps));
    }

    default RebalanceListener<K,V> andThen(RebalanceListener<K,V> following) {
        return following.compose(this);
    }

}
