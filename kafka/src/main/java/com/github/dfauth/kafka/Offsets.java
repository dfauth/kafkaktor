package com.github.dfauth.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

public class Offsets {

    private final TopicPartition topicPartition;
    private final Long beginningOffset;
    private final Long currentOffset;
    private final Long endOffset;

    public Offsets(TopicPartition tp, Long beginningOffset, Long currentOffset, Long endOffset) {
        this.topicPartition = tp;
        this.beginningOffset = beginningOffset;
        this.currentOffset = currentOffset;
        this.endOffset = endOffset;
    }

    public TopicPartition getTopicPartition() {
        return topicPartition;
    }

    public Long getBeginningOffset() {
        return beginningOffset;
    }

    public Long getCurrentOffset() {
        return currentOffset;
    }

    public Long getEndOffset() {
        return endOffset;
    }

    public boolean isReset(ConsumerRecord<?,?> record) {
        return record.offset() <= beginningOffset;
    }

    public boolean isRecovering(ConsumerRecord<?,?> record) {
        return record.offset() < currentOffset;
    }

    public boolean isLagging(ConsumerRecord<?,?> record) {
        return lag(record) > 0;
    }

    public long lag(ConsumerRecord<?,?> record) {
        return endOffset - record.offset();
    }
}
