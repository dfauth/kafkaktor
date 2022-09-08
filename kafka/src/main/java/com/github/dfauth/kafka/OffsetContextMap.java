package com.github.dfauth.kafka;

import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class OffsetContextMap implements Function<TopicPartition, Offsets> {

    private final CompletableFuture<Map<TopicPartition, Offsets>> f;
    private final Map<TopicPartition, Offsets> map = new HashMap<>();

    public OffsetContextMap(CompletableFuture<Map<TopicPartition, Offsets>> f) {
        this.f = f;
    }

    public void assign(Map<TopicPartition, Offsets> assignments) {
        map.putAll(assignments);
        if(!f.isDone()) {
            f.complete(map);
        }
    }

    public void revoke(Collection<TopicPartition> revocations) {
        revocations.forEach(map::remove);
    }

    @Override
    public Offsets apply(TopicPartition topicPartition) {
        return map.get(topicPartition);
    }
}
