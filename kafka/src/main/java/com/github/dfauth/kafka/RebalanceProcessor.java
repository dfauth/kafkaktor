package com.github.dfauth.kafka;

import com.github.dfauth.trycatch.TryCatch;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.github.dfauth.functional.Maps.mapTransformerOf;

public interface RebalanceProcessor<K,V,T> extends KafkaConsumerAware<Function<Collection<TopicPartition>,T>, K,V>{

    static <K,V> RebalanceProcessor<K,V, Map<TopicPartition, Offsets>> offsets() {
        return c -> tps -> {
            Map<TopicPartition, Long> beginningOffsets = c.beginningOffsets(tps);
            Map<TopicPartition, Long> endOffsets = c.beginningOffsets(tps);
            BiFunction<TopicPartition, Long, Offsets> f = (tp, o) -> new Offsets(o, c.position(tp), endOffsets.get(tp));
            return mapTransformerOf(f).apply(beginningOffsets);
        };
    }

    default RebalanceProcessor<K,V,T> andThen(RebalanceListener<K,V> nested) {
        return c -> tps ->
            TryCatch.CallableBuilder.tryCatch((Callable<T>) () -> withKafkaConsumer(c).apply(tps))
                    .andFinallyRun(() -> nested.withKafkaConsumer(c).accept(tps));
    }
}
