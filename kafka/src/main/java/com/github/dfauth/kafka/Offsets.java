package com.github.dfauth.kafka;

public class Offsets {

    private final Long beginningOffset;
    private final Long currentOffset;
    private final Long endOffset;

    public Offsets(Long beginningOffset, Long currentOffset, Long endOffset) {
        this.beginningOffset = beginningOffset;
        this.currentOffset = currentOffset;
        this.endOffset = endOffset;
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

    public boolean isReset(Long offset) {
        return offset <= beginningOffset;
    }

    public boolean isRecovering(Long offset) {
        return offset < currentOffset;
    }

    public boolean isLagging(Long offset) {
        return lag(offset) > 0;
    }

    public long lag(Long offset) {
        return endOffset - offset;
    }
}
