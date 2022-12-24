package com.github.dfauth.kafka.recovery;

import org.apache.kafka.common.TopicPartition;

public interface RecoveryState {

    static RecoveryState initial(TopicPartition tp, long initialOffset, long currentOffset, PartitionRecoveryListener recoveryListener) {
        return initialOffset > currentOffset ? new InitialRecoveryState(tp, currentOffset, recoveryListener) : new RecoveredState(tp, currentOffset, recoveryListener);
    }

    RecoveryState dispatch(long offset);
}
