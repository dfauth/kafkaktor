package com.github.dfauth.kafka.recovery;

import org.apache.kafka.common.TopicPartition;

public class RecoveredState extends InitialRecoveryState {

    public RecoveredState(TopicPartition tp, long offset, PartitionRecoveryListener recoveryListener) {
        super(tp, offset, recoveryListener);
        this.recoveryListener.recovered(tp, offset);
    }

    @Override
    public RecoveryState dispatch(long offset) {
        return this;
    }
}
