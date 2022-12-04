package com.github.dfauth.kafka.recovery;

import org.apache.kafka.common.TopicPartition;

public class RecoveringState extends InitialRecoveryState {

    public RecoveringState(TopicPartition tp, long offset, PartitionRecoveryListener recoveryListener) {
        super(tp, offset, recoveryListener);
        this.recoveryListener.recovering(tp,offset);
    }

    @Override
    public RecoveryState dispatch(long offset) {
        return offset < this.offset ? this : new RecoveredState(tp, offset, this.recoveryListener);
    }
}
