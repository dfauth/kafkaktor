package com.github.dfauth.kafka.recovery;

import org.apache.kafka.common.TopicPartition;

public class RecoveryEvent {

    private final long offset;
    private final TopicPartition tp;

    public RecoveryEvent(TopicPartition tp, long offset) {
        this.tp = tp;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return String.format("%s[%s,%d]", RecoveryEvent.class.getSimpleName(), tp, offset);
    }
}
