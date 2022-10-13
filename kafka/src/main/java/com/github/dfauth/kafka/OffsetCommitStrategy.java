package com.github.dfauth.kafka;

import com.github.dfauth.functional.Maps;
import com.github.dfauth.functional.Reducer;
import com.github.dfauth.functional.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.github.dfauth.functional.Collectors.tuple2Collector;
import static com.github.dfauth.functional.Lists.extendedList;
import static com.github.dfauth.functional.Lists.segment;
import static com.github.dfauth.functional.Maps.extendedMap;
import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatchIgnore;
import static java.util.function.Predicate.not;

interface OffsetCommitStrategy {

    void commit(Map<TopicPartition, OffsetAndMetadata> m);

    default void commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records) {
        Optional.of(records.stream()
                    .filter(e -> e.getValue().isDone())                                 // filter for completed futures (should be all)
                    .map(Tuple2::tuple2)                                                // turn into a tuple
                    .map(t -> t.mapRight(tryCatch(CompletableFuture::get)))             // map the tuple, converting the future to its completed value
                    .collect(tuple2Collector()))                                        // collect tuples to a map
                .filter(not(Map::isEmpty))                                              // filter for empty maps
                .ifPresent(this::commit);                                               // commit
    }

    interface Factory<K,V> extends KafkaConsumerAware<OffsetCommitStrategy,K,V> {

        static <K,V> Factory<K,V> sync() {
            return SyncOffsetCommitStrategy::new;
        }
        static <K,V> Factory<K,V> async() {
            return AsyncOffsetCommitStrategy::new;
        }
        static <K,V> Factory<K,V> externalSync() {
            return ExternalSyncOffsetCommitStrategy::new;
        }
        static <K,V> Factory<K,V> externalAsync() {
            return ExternalAsyncOffsetCommitStrategy::new;
        }

        default KafkaConsumerAware<OffsetCommitStrategy,K,V> compose(Factory<K,V> next) {
            return next.andThen(this);
        }

        default KafkaConsumerAware<OffsetCommitStrategy,K,V> andThen(KafkaConsumerAware<OffsetCommitStrategy,K,V> next) {
            return c -> {
                OffsetCommitStrategy tmp = withKafkaConsumer(c);
                OffsetCommitStrategy tmp1 = next.withKafkaConsumer(c);
                    return m -> {
                        tryCatchIgnore(() -> tmp.commit(m));
                        tryCatchIgnore(() -> tmp1.commit(m));
                    };
            };
        }
    }

    class SyncOffsetCommitStrategy<K,V> implements OffsetCommitStrategy {

        private final KafkaConsumer<K, V> consumer;

        public SyncOffsetCommitStrategy(KafkaConsumer<K,V> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void commit(Map<TopicPartition, OffsetAndMetadata> m) {
            if(m.size() > 0) {
                consumer.commitSync();
            }
        }

        @Override
        public void commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records) {
        }
    }

    @Slf4j
    class AsyncOffsetCommitStrategy<K,V> implements OffsetCommitStrategy {

        private final KafkaConsumer<K, V> consumer;

        public AsyncOffsetCommitStrategy(KafkaConsumer<K,V> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void commit(Map<TopicPartition, OffsetAndMetadata> m) {
            consumer.commitAsync((o,e) -> Optional.ofNullable(e).ifPresent(_e -> log.error(_e.getMessage(), _e)));
        }

        @Override
        public void commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records) {
        }
    }

    class ExternalSyncOffsetCommitStrategy<K,V> implements OffsetCommitStrategy {

        private final KafkaConsumer<K, V> consumer;
        private Maps.ExtendedMap<TopicPartition, List<CompletableFuture<OffsetAndMetadata>>> pending = extendedMap(new HashMap<>());

        public ExternalSyncOffsetCommitStrategy(KafkaConsumer<K,V> consumer) {
            this.consumer = consumer;
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
            m.forEach((k,v) -> pending.compute(k, (k1, v1) ->
                    Optional.ofNullable(v1).map(_v1 ->
                            extendedList(_v1).append(v)
                    ).orElse(v)
            ));
            // segment acks up to first incomplete ack
            Tuple2<Maps.ExtendedMap<TopicPartition,List<CompletableFuture<OffsetAndMetadata>>>, Maps.ExtendedMap<TopicPartition,List<CompletableFuture<OffsetAndMetadata>>>> m1 = pending.foldLeft(
                    Tuple2.tuple2(extendedMap(), extendedMap()),
                    acc -> (k,v) -> segment(v, CompletableFuture::isDone) // Tuple2 of List of futures
                            .map((t1,t2) -> Tuple2.tuple2(
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
    class ExternalAsyncOffsetCommitStrategy<K,V> implements OffsetCommitStrategy {

        private final KafkaConsumer<K, V> consumer;

        public ExternalAsyncOffsetCommitStrategy(KafkaConsumer<K,V> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void commit(Map<TopicPartition, OffsetAndMetadata> m) {
            consumer.commitAsync(m, (o,e) -> Optional.ofNullable(e).ifPresent(_e -> log.error(_e.getMessage(), _e)));
        }
    }
}
