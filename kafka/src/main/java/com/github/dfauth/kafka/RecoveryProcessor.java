package com.github.dfauth.kafka;

import com.github.dfauth.functional.Maps;
import com.github.dfauth.kafka.recovery.PartitionRecoveryListener;
import com.github.dfauth.kafka.recovery.RecoveryState;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.dfauth.kafka.recovery.RecoveryState.initial;

public interface RecoveryProcessor<K,V> extends RebalanceFunction<K,V, Collection<TopicPartition>, Map<TopicPartition, RecoveryState>> {

    static <K,V> RecoveryProcessor<K,V> rebalanceListener(RebalanceProcessor<K,V> watermarkOperator, PartitionRecoveryListener recoveryListener, RebalanceListener<K,V> rewindOperator) {
        return c -> {
            Function<Collection<TopicPartition>, Map<TopicPartition, Long>> _watermarkOperator = watermarkOperator.withKafkaConsumer(c);
            Consumer<Collection<TopicPartition>> _rewindOperator = rewindOperator.withKafkaConsumer(c);
            return tps -> {
                recoveryListener.partitionsAssigned(tps);
                Map<TopicPartition, Long> offsets = _watermarkOperator.apply(tps);
                _rewindOperator.accept(tps);
                return Maps.map(offsets, (tp, o) -> Map.entry(tp, initial(tp, o,c.position(tp), recoveryListener)));
            };
        };
    }


}
