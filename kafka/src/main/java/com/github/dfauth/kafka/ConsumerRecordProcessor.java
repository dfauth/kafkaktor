package com.github.dfauth.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ConsumerRecordProcessor<T, R> extends Function<ConsumerRecord<T, R>, Map.Entry<TopicPartition,CompletableFuture<OffsetAndMetadata>>> {

    static <T, R> ConsumerRecordProcessor<T, R> toConsumerRecordProcessor(Consumer<ConsumerRecord<T, R>> recordConsumer) {

        CompletableFuture<OffsetAndMetadata> DUMMY = CompletableFuture.completedFuture(new OffsetAndMetadata(0L));

        return r -> {
            recordConsumer.accept(r);
            return Map.entry(new TopicPartition(r.topic(), r.partition()), DUMMY);
        };
    }
}
