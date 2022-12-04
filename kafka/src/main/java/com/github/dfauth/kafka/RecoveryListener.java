package com.github.dfauth.kafka;

import com.github.dfauth.kafka.recovery.PartitionRecoveryListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.*;
import java.util.function.Consumer;

public class RecoveryListener<K, V> implements RebalanceListener<K, V>, PartitionRecoveryListener {

    static <K,V> RecoveryListener<K,V> recoveryListener() {
        return new RecoveryListener<>();
    }

    private List<TopicPartition> partitions = new ArrayList<>();
    private Map<TopicPartition, Long> recovered = new HashMap<>();
    private Consumer<Map<TopicPartition, Long>> consumer;

    @Override
    public Consumer<Collection<TopicPartition>> withKafkaConsumer(KafkaConsumer<K, V> consumer) {
        return tps -> partitions.addAll(tps);
    }

    public void recovered(TopicPartition tp, long offset) {
        partitions.remove(tp);
        recovered.put(tp, offset);
        if (partitions.isEmpty()) {
            Optional.ofNullable(consumer).ifPresent(c -> c.accept(recovered));
        }
    }

    public PartitionRecoveryListener onRecovery(Consumer<Map<TopicPartition, Long>> consumer) {
        this.consumer = consumer;
        return this;
    }
}
