package com.github.dfauth.kafka;

import com.github.dfauth.kafka.recovery.PartitionRecoveryListener;
import org.apache.kafka.common.TopicPartition;

import java.util.*;
import java.util.function.Consumer;

public class RecoveryListener<K,V> implements PartitionRecoveryListener {

    private final Map<TopicPartition, Long> recovering = new HashMap<>();
    private final Map<TopicPartition, Long> recovered = new HashMap<>();
    private Consumer<Map<TopicPartition, Long>> consumer;
    private final Collection<TopicPartition> assigned = new ArrayList<>();

    public RecoveryListener() {
    }

    @Override
    public void partitionsAssigned(Collection<TopicPartition> tps) {
        this.assigned.addAll(tps);
    }

    @Override
    public void recovering(TopicPartition tp, long offset) {
        assigned.remove(tp);
        recovering.put(tp,offset);
    }

    public void recovered(TopicPartition tp, long offset) {
        recovering.remove(tp);
        assigned.remove(tp);
        recovered.put(tp, offset);
        if (recovering.isEmpty() && assigned.isEmpty()) {
            Optional.ofNullable(consumer).ifPresent(c -> c.accept(recovered));
        }
    }

    public PartitionRecoveryListener onRecovery(Consumer<Map<TopicPartition, Long>> consumer) {
        this.consumer = consumer;
        return this;
    }

}
