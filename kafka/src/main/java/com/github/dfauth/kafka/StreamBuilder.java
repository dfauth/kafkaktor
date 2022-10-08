package com.github.dfauth.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.github.dfauth.functional.Functions.peek;
import static com.github.dfauth.kafka.ConsumerRecordProcessor.toConsumerRecordProcessor;
import static com.github.dfauth.kafka.utils.KafkaUtils.wrapConsumerRecord;
import static java.util.function.Function.identity;

public class StreamBuilder<K,V,T,R> {

    private Map<String, Object> props;
    private String topic;
    private ConsumerRecordProcessor<T,R> recordProcessor;
    private Deserializer<K> keyDeserializer;
    private Deserializer<V> valueDeserializer;
    private Function<K,T> keyMapper;
    private Function<V,R> valueMapper;
    private Duration pollingDuration = Duration.ofMillis(50);
    private RebalanceListener<K,V> partitionRevocationListener = consumer -> topicPartitions -> {};
    private RebalanceListener<K,V> partitionAssignmentListener = consumer -> topicPartitions -> {};
    private CommitStrategy.Factory commitStrategy = CommitStrategy.Factory.SYNC;
    private Predicate<T> keyFilter = t -> true;
    private ExecutorService executor;

    public static <K,V> StreamBuilder<K,V,K,V> unmappedBuilder() {
        return new StreamBuilder<K,V,K,V>().withKeyMapper(identity()).withValueMapper(identity());
    }

    public static <V,R> StreamBuilder<String,V,String,R> stringKeyBuilder() {
        return new StreamBuilder<String,V,String,R>().withKeyDeserializer(new StringDeserializer()).withKeyMapper(identity());
    }

    public static <V> StreamBuilder<String,V,String,V> stringKeyUnmappedValueBuilder() {
        return StreamBuilder.<V,V>stringKeyBuilder().withValueMapper(identity());
    }

    public static <K,V,T,R> StreamBuilder<K,V,T,R> builder() {
        return new StreamBuilder<>();
    }

    public StreamBuilder<K,V,T,R> withKeyFilter(Predicate<T> keyFilter) {
        this.keyFilter = keyFilter;
        return this;
    }

    public StreamBuilder<K,V,T,R> withExecutor(ThreadPoolExecutor executor) {
        this.executor = executor;
        return this;
    }

    public StreamBuilder<K,V,T,R> withProperties(Map<String, Object> props, String key, Object value) {
        return withProperties(props, Collections.singletonMap(key, value));
    }

    @SafeVarargs
    public final StreamBuilder<K,V,T,R> withProperties(Map<String, Object>... props) {
        this.props = Arrays.stream(props).reduce(new HashMap<>(), (m1, m2) -> {
            m1.putAll(m2);
            return m1;
        });
        return this;
    }

    public StreamBuilder<K,V,T,R> withTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public StreamBuilder<K,V,T,R> withKeyDeserializer(Deserializer<K> deserializer) {
        this.keyDeserializer = deserializer;
        return this;
    }

    public StreamBuilder<K,V,T,R> withValueDeserializer(Deserializer<V> deserializer) {
        this.valueDeserializer = deserializer;
        return this;
    }

    public StreamBuilder<K,V,T,R> withKeyMapper(Function<K,T> keyMapper) {
        this.keyMapper = keyMapper;
        return this;
    }

    public StreamBuilder<K,V,T,R> withValueMapper(Function<V,R> valueMapper) {
        this.valueMapper = valueMapper;
        return this;
    }

    public StreamBuilder<K,V,T,R> withRecordProcessor(ConsumerRecordProcessor<T, R> recordProcessor) {
        this.recordProcessor = recordProcessor;
        return this;
    }

    public StreamBuilder<K,V,T,R> withRecordConsumer(Consumer<ConsumerRecord<T,R>> recordConsumer) {
        this.recordProcessor = toConsumerRecordProcessor(recordConsumer);
        return this;
    }

    public StreamBuilder<K,V,T,R> withKeyValueConsumer(BiConsumer<T,R> keyValueConsumer) {
        return withRecordConsumer(r -> keyValueConsumer.accept(r.key(), r.value()));
    }

    public StreamBuilder<K,V,T,R> withValueConsumer(Consumer<R> valueConsumer) {
        return withKeyValueConsumer((ignored, v) -> valueConsumer.accept(v));
    }

    public StreamBuilder<K,V,T,R> withPollingDuration(Duration duration) {
        this.pollingDuration = duration;
        return this;
    }

    public StreamBuilder<K,V,T,R> onPartitionAssignment(RebalanceListener<K,V> partitionAssignmentListener) {
        this.partitionAssignmentListener = partitionAssignmentListener;
        return this;
    }

    public StreamBuilder<K,V,T,R> onPartitionRevocation(RebalanceListener<K,V> partitionRevocationListener) {
        this.partitionRevocationListener = partitionRevocationListener;
        return this;
    }

    public StreamBuilder<K,V,T,R> withCommitStrategy(CommitStrategy.Factory commitStrategy) {
        this.commitStrategy = commitStrategy;
        return this;
    }

    public KafkaStream<K,V> build() {
        ConsumerRecordProcessor<T, R> rp = recordProcessor;
        Predicate<K> kf = k -> keyFilter.test(keyMapper.apply(k));
        executor = Optional.ofNullable(executor).orElseGet(KafkaExecutors::executor);
        return new KafkaStream<>(new HashMap<>(this.props), this.topic, this.keyDeserializer, this.valueDeserializer, r -> rp.apply(wrapConsumerRecord(r,keyMapper,valueMapper)), pollingDuration, partitionAssignmentListener, partitionRevocationListener, commitStrategy, executor, kf);
    }

    @Slf4j
    public static class KafkaStream<K,V> implements Runnable, AutoCloseable {

        private final Map<String, Object> props;
        private final String topic;
        private final Deserializer<K> keyDeserializer;
        private final Deserializer<V> valueDeserializer;
        private final Duration duration;
        private final ConsumerRecordProcessor<K, V> recordProcessor;
        private final AtomicBoolean isRunning = new AtomicBoolean(false);
        private final ExecutorService executor;
        private final Duration timeout;
        private final RebalanceListener<K,V> partitionRevocationListener;
        private final RebalanceListener<K,V> partitionAssignmentListener;
        private final CommitStrategy.Factory commitStrategyFactory;
        private KafkaConsumer<K,V> consumer;
        private CommitStrategy commitStrategy;
        private final Predicate<ConsumerRecord<K,V>> keyFilter;

        public KafkaStream(Map<String, Object> props, String topic, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer, ConsumerRecordProcessor<K, V> recordProcessor, Duration duration, RebalanceListener<K,V> partitionAssignmentListener, RebalanceListener<K,V> partitionRevocationListener, CommitStrategy.Factory commitStrategy, ExecutorService executor, Predicate<K> keyFilter) {
            this(props, topic, keyDeserializer, valueDeserializer, recordProcessor, duration, Duration.ofMillis(1000), partitionAssignmentListener, partitionRevocationListener, commitStrategy, executor, keyFilter);
        }

        public KafkaStream(Map<String, Object> props, String topic, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer, ConsumerRecordProcessor<K, V> recordProcessor, Duration duration, Duration timeout, RebalanceListener<K,V> partitionAssignmentListener, RebalanceListener<K,V> partitionRevocationListener, CommitStrategy.Factory commitStrategy, ExecutorService executor, Predicate<K> keyFilter) {
            this.props = props;
            this.topic = topic;
            this.keyDeserializer = keyDeserializer;
            this.valueDeserializer = valueDeserializer;
            this.duration = duration;
            this.recordProcessor = recordProcessor;
            this.timeout = timeout;
            this.partitionAssignmentListener = partitionAssignmentListener;
            this.partitionRevocationListener = partitionRevocationListener;
            this.commitStrategyFactory = commitStrategy;
            this.props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            this.executor = executor;
            this.keyFilter = r -> keyFilter.test(r.key());
        }

        public CompletableFuture<Map<TopicPartition, Offsets>> start() {
            CompletableFuture<Map<TopicPartition, Offsets>> f = new CompletableFuture<>();
            OffsetContextMap offsetCtx = new OffsetContextMap(f);
            isRunning.set(true);
            consumer = new KafkaConsumer<>(props, keyDeserializer, valueDeserializer);
            commitStrategy = commitStrategyFactory.create(consumer);
            UnaryOperator<Collection<TopicPartition>> x = peek(partitionRevocationListener.withKafkaConsumer(consumer));
            Function<Collection<TopicPartition>, Map<TopicPartition, Offsets>> y = RebalanceProcessor.<K, V>offsets().andThen(partitionAssignmentListener).withKafkaConsumer(consumer);
            consumer.subscribe(Collections.singleton(topic), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    offsetCtx.revoke(x.apply(partitions));
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    offsetCtx.assign(y.apply(partitions));
                }
            });
            process();
            return f;
        }

        private void process() {
            executor.submit(this);
        }

        public void run() {
            List<Map.Entry<TopicPartition,CompletableFuture<OffsetAndMetadata>>> processed;
            do {
                processed = StreamSupport.stream(consumer.poll(duration).spliterator(), false)
                        .filter(keyFilter)
                        .map(recordProcessor)
                        .collect(Collectors.toList());
                commitStrategy.commit(processed);
            } while(!this.yield(processed) && isRunning.get());
            if(!isRunning.get()) {
                consumer.close(timeout);
            } else {
                executor.submit(this);
            }
        }

        protected boolean yield(List<Map.Entry<TopicPartition,CompletableFuture<OffsetAndMetadata>>> processed) {
            return processed.stream().map(Map.Entry::getValue).anyMatch(CompletableFuture::isDone);
        }

        public void stop() {
            isRunning.set(false);
        }

        @Override
        public void close() {
            stop();
        }
    }
}
