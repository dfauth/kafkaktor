package com.github.dfauth.kafka;

import com.github.dfauth.functional.Maps;
import com.github.dfauth.functional.Reducer;
import com.github.dfauth.functional.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.github.dfauth.functional.Collectors.mapEntryCollector;
import static com.github.dfauth.functional.Lists.extendedList;
import static com.github.dfauth.functional.Lists.segment;
import static com.github.dfauth.functional.Maps.extendedMap;
import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatchIgnore;
import static java.util.function.Predicate.not;

interface OffsetCommitStrategy {

    Map<TopicPartition, OffsetAndMetadata> commit(Map<TopicPartition, OffsetAndMetadata> m);

    default Map<TopicPartition, OffsetAndMetadata> commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records) {
        return Optional.of(records.stream()
                    .filter(e -> e.getValue().isDone())                                   // filter for completed futures (should be all)
                    .map(e -> Map.entry(e.getKey(), tryCatch(() -> e.getValue().get())))  // map the tuple, converting the future to its completed value
                    .collect(mapEntryCollector()))                                        // collect to a map
                .filter(not(Map::isEmpty))                                                // filter for empty maps
                .map(this::commit).orElse(Collections.emptyMap());                                                 // commit
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
                        return m;
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
        public Map<TopicPartition, OffsetAndMetadata> commit(Map<TopicPartition, OffsetAndMetadata> m) {
            if(m.size() > 0) {
                consumer.commitSync();
            }
            return m;
        }

        @Override
        public Map<TopicPartition, OffsetAndMetadata> commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records) {
            return null;
        }
    }

    @Slf4j
    class AsyncOffsetCommitStrategy<K,V> implements OffsetCommitStrategy {

        private final KafkaConsumer<K, V> consumer;

        public AsyncOffsetCommitStrategy(KafkaConsumer<K,V> consumer) {
            this.consumer = consumer;
        }

        @Override
        public Map<TopicPartition, OffsetAndMetadata> commit(Map<TopicPartition, OffsetAndMetadata> m) {
            consumer.commitAsync((o,e) -> Optional.ofNullable(e).ifPresent(_e -> log.error(_e.getMessage(), _e)));
            return m;
        }

        @Override
        public Map<TopicPartition, OffsetAndMetadata> commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records) {
            return commit(Collections.emptyMap());
        }
    }

    class ExternalSyncOffsetCommitStrategy<K,V> implements OffsetCommitStrategy {

        private final KafkaConsumer<K, V> consumer;
        private Maps.ExtendedMap<TopicPartition, List<CompletableFuture<OffsetAndMetadata>>> pending = extendedMap(new HashMap<>());

        public ExternalSyncOffsetCommitStrategy(KafkaConsumer<K,V> consumer) {
            this.consumer = consumer;
        }

        @Override
        public Map<TopicPartition, OffsetAndMetadata> commit(Map<TopicPartition, OffsetAndMetadata> m) {
            consumer.commitSync(m);
            return m;
        }

        @Override
        public Map<TopicPartition, OffsetAndMetadata> commit(List<Map.Entry<TopicPartition, CompletableFuture<OffsetAndMetadata>>> records) {
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
            m1._1().forEach((k,v) -> extendedList(v).reverse().head().thenAccept(o -> commits.put(k, o)));
            commit(commits);
            return null;
        }
    }

    @Slf4j
    class ExternalAsyncOffsetCommitStrategy<K,V> implements OffsetCommitStrategy {

        private final KafkaConsumer<K, V> consumer;

        public ExternalAsyncOffsetCommitStrategy(KafkaConsumer<K,V> consumer) {
            this.consumer = consumer;
        }

        @Override
        public Map<TopicPartition, OffsetAndMetadata> commit(Map<TopicPartition, OffsetAndMetadata> m) {
            consumer.commitAsync(m, (o,e) -> Optional.ofNullable(e).ifPresent(_e -> log.error(_e.getMessage(), _e)));
            return m;
        }
    }
}
