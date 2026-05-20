package com.github.dfauth.kafka.recovery;

import org.apache.kafka.common.TopicPartition;

public interface RecoveryState {

    static RecoveryState initial(TopicPartition tp, long offset, PartitionRecoveryListener recoveryListener) {
        return new InitialRecoveryState(tp, offset, recoveryListener);
    }

    RecoveryState dispatch(long offset);
}
