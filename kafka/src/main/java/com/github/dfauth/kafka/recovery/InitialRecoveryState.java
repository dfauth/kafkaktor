package com.github.dfauth.kafka.recovery;

import org.apache.kafka.common.TopicPartition;

public class InitialRecoveryState implements RecoveryState {

    protected final TopicPartition tp;
    protected final long offset;
    protected final PartitionRecoveryListener recoveryListener;

    public InitialRecoveryState(TopicPartition tp, long offset, PartitionRecoveryListener recoveryListener) {
        this.tp = tp;
        this.offset = offset;
        this.recoveryListener = recoveryListener;
    }

    @Override
    public RecoveryState dispatch(long offset) {
        return offset < this.offset ? new RecoveringState(tp, offset, this.recoveryListener) : new RecoveredState(tp, offset, this.recoveryListener);
    }

}
