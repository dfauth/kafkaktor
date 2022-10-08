package com.github.dfauth.kafka;

import com.github.dfauth.functional.Maps;
import com.github.dfauth.functional.Reducer;
import com.github.dfauth.functional.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.github.dfauth.functional.Lists.extendedList;
import static com.github.dfauth.functional.Lists.segment;
import static com.github.dfauth.functional.Maps.extendedMap;

abstract class CommitStrategy {

    protected KafkaConsumer<?, ?> consumer;

    public CommitStrategy(KafkaConsumer<?, ?> consumer) {
        this.consumer = consumer;
    }

    public final void commit(String topic, int partition, long offset) {
        commit(new TopicPartition(topic, partition), new OffsetAndMetadata(offset));
    }

    public final void commit(ConsumerRecord<?,?> record) {
        commit(record.topic(), record.partition(), record.offset());
    }

    public void commit(TopicPartition tp, OffsetAndMetadata om) {
        commit(Collections.singletonMap(tp, om));
    }

    public abstract void commit(Map<TopicPartition, OffsetAndMetadata> m);

    public abstract void commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records);

    enum Factory {
        SYNC(c -> new SyncCommitStrategy(c)),
        ASYNC(c -> new AsyncCommitStrategy(c)),
        EXTERNAL_SYNC(c -> new ExternalSyncCommitStrategy(c)),
        EXTERNAL_ASYNC(c -> new ExternalAsyncCommitStrategy(c));

        private final Function<KafkaConsumer<?, ?>, CommitStrategy> f;

        Factory(Function<KafkaConsumer<?, ?>, CommitStrategy> f) {
            this.f = f;
        }

        public CommitStrategy create(KafkaConsumer<?,?> consumer) {
            return f.apply(consumer);
        }
    }

    private static class SyncCommitStrategy extends CommitStrategy {

        public SyncCommitStrategy(KafkaConsumer<?, ?> consumer) {
            super(consumer);
        }

        @Override
        public void commit(Map<TopicPartition, OffsetAndMetadata> m) {
            consumer.commitSync();
        }

        @Override
        public void commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records) {
        }
    }

    @Slf4j
    private static class AsyncCommitStrategy extends CommitStrategy {

        public AsyncCommitStrategy(KafkaConsumer<?, ?> consumer) {
            super(consumer);
        }

        @Override
        public void commit(Map<TopicPartition, OffsetAndMetadata> m) {
            consumer.commitAsync((o,e) -> Optional.ofNullable(e).ifPresent(_e -> log.error(_e.getMessage(), _e)));
        }

        @Override
        public void commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records) {
        }
    }

    private static class ExternalSyncCommitStrategy extends CommitStrategy {

        private Maps.ExtendedMap<TopicPartition, List<CompletableFuture<OffsetAndMetadata>>> pending = extendedMap(new HashMap<>());

        public ExternalSyncCommitStrategy(KafkaConsumer<?, ?> consumer) {
            super(consumer);
        }

        @Override
        public void commit(Map<TopicPartition, OffsetAndMetadata> m) {
            consumer.commitSync(m);
        }

        @Override
        public void commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records) {
            // group values by TopicPartition
            Map<TopicPartition, List<CompletableFuture<OffsetAndMetadata>>> m = Reducer.<TopicPartition, CompletableFuture<OffsetAndMetadata>>mapEntryGroupingMappingReducer().reduce(records);

            // for each group, append to pending acks
            m.forEach((k,v) -> {
                pending.compute(k, (k1,v1) ->
                        Optional.ofNullable(v1).map(_v1 ->
                                extendedList(_v1).append(v)
                        ).orElse(v)
                );
            });
            // segment acks up to first incomplete ack
            Tuple2<Maps.ExtendedMap<TopicPartition,List<CompletableFuture<OffsetAndMetadata>>>, Maps.ExtendedMap<TopicPartition,List<CompletableFuture<OffsetAndMetadata>>>> m1 = pending.foldLeft(
                    Tuple2.of(extendedMap(), extendedMap()),
                    acc -> (k,v) -> segment(v, CompletableFuture::isDone) // Tuple2 of List of futures
                            .map((t1,t2) -> Tuple2.of(
                                            extendedMap(acc._1()).mergeEntry(k, t1,(v1, v2) -> extendedList(v1).append(v2)),
                                            extendedMap(acc._2()).mergeEntry(k, t2,(v1, v2) -> extendedList(v1).append(v2))
                                    )
                            )
            );
            // second part of the tuple are the new pending futures
            pending = m1._2();
            // first part of the offsets to be synced
            // reduce to the maximum offset
            HashMap<TopicPartition, OffsetAndMetadata> commits = new HashMap<>();
            m1._1().map((k,v) -> extendedList(v).reverse().head().thenAccept(o -> commits.put(k, o)));
            commit(commits);
        }
    }

    @Slf4j
    private static class ExternalAsyncCommitStrategy extends ExternalSyncCommitStrategy {

        public ExternalAsyncCommitStrategy(KafkaConsumer<?, ?> consumer) {
            super(consumer);
        }

        @Override
        public void commit(Map<TopicPartition, OffsetAndMetadata> m) {
            consumer.commitAsync(m, (o,e) -> Optional.ofNullable(e).ifPresent(_e -> log.error(_e.getMessage(), _e)));
        }
    }
}
