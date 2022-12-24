package com.github.dfauth.kafka.recovery;

import org.apache.kafka.common.TopicPartition;

import java.util.Collection;

@FunctionalInterface
public interface PartitionRecoveryListener {

    default void recovering(TopicPartition tp, long offset) {
    }

    void recovered(TopicPartition tp, long offset);

    default void partitionsAssigned(Collection<TopicPartition> tps) {
    }
}
