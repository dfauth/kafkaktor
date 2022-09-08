package com.github.dfauth.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.stream.StreamSupport;

import static com.github.dfauth.functional.Function2.peek;
import static com.github.dfauth.kafka.utils.KafkaUtils.wrapConsumerRecord;
import static com.github.dfauth.trycatch.TryCatch._Callable;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatchIgnore;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;

public class StreamBuilder<K,V,T,R> {

    private Map<String, Object> props;
    private String topic;
    private Consumer<ConsumerRecord<T,R>> recordConsumer;
    private Deserializer<K> keyDeserializer;
    private Deserializer<V> valueDeserializer;
    private Function<K,T> keyMapper;
    private Function<V,R> valueMapper;
    private Duration pollingDuration = Duration.ofMillis(50);
    private RebalanceListener<K,V> partitionRevocationListener = consumer -> topicPartitions -> {};
    private RebalanceListener<K,V> partitionAssignmentListener = consumer -> topicPartitions -> {};
    private CommitStrategy.Factory commitStrategy = CommitStrategy.Factory.SYNC;
    private Predicate<T> keyFilter = t -> true;
    private BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(10);
    private ThreadPoolExecutor executor;

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

    public StreamBuilder<K,V,T,R> withQueue(BlockingQueue<Runnable> queue) {
        this.queue = queue;
        return this;
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

    public StreamBuilder<K,V,T,R> withRecordConsumer(Consumer<ConsumerRecord<T,R>> recordConsumer) {
        this.recordConsumer = recordConsumer;
        return this;
    }

    public StreamBuilder<K,V,T,R> withKeyValueConsumer(BiConsumer<T,R> keyValueConsumer) {
        this.recordConsumer = r -> keyValueConsumer.accept(r.key(), r.value());
        return this;
    }

    public StreamBuilder<K,V,T,R> withValueConsumer(Consumer<R> valueConsumer) {
        this.recordConsumer = r -> valueConsumer.accept(r.value());
        return this;
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
        Consumer<ConsumerRecord<T, R>> rc = recordConsumer;
        Predicate<K> kf = k -> keyFilter.test(keyMapper.apply(k));
        executor = Optional.ofNullable(executor).orElseGet(() -> {
            ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 60, SECONDS, queue);
            pool.prestartCoreThread();
            return pool;
        });
        return new KafkaStream<>(new HashMap<>(this.props), this.topic, this.keyDeserializer, this.valueDeserializer, r -> rc.accept(wrapConsumerRecord(r,keyMapper,valueMapper)), pollingDuration, partitionAssignmentListener, partitionRevocationListener, commitStrategy, executor.getQueue(), kf);
    }

    @Slf4j
    public static class KafkaStream<K,V> implements Runnable {

        private final Map<String, Object> props;
        private final String topic;
        private final Deserializer<K> keyDeserializer;
        private final Deserializer<V> valueDeserializer;
        private final Duration duration;
        private final Consumer<ConsumerRecord<K,V>> recordConsumer;
        private final AtomicBoolean isRunning = new AtomicBoolean(false);
        private final BlockingQueue<Runnable> queue;
        private final Duration timeout;
        private final RebalanceListener<K,V> partitionRevocationListener;
        private final RebalanceListener<K,V> partitionAssignmentListener;
        private final CommitStrategy.Factory commitStrategyFactory;
        private KafkaConsumer<K,V> consumer;
        private CommitStrategy commitStrategy;
        private Predicate<ConsumerRecord<K,V>> keyFilter;

        public KafkaStream(Map<String, Object> props, String topic, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer, Consumer<ConsumerRecord<K,V>> recordConsumer, Duration duration, RebalanceListener<K,V> partitionAssignmentListener, RebalanceListener<K,V> partitionRevocationListener, CommitStrategy.Factory commitStrategy, BlockingQueue<Runnable> queue, Predicate<K> keyFilter) {
            this(props, topic, keyDeserializer, valueDeserializer, recordConsumer, duration, Duration.ofMillis(1000), partitionAssignmentListener, partitionRevocationListener, commitStrategy, queue, keyFilter);
        }

        public KafkaStream(Map<String, Object> props, String topic, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer, Consumer<ConsumerRecord<K,V>> recordConsumer, Duration duration, Duration timeout, RebalanceListener<K,V> partitionAssignmentListener, RebalanceListener<K,V> partitionRevocationListener, CommitStrategy.Factory commitStrategy, BlockingQueue<Runnable> queue, Predicate<K> keyFilter) {
            this.props = props;
            this.topic = topic;
            this.keyDeserializer = keyDeserializer;
            this.valueDeserializer = valueDeserializer;
            this.duration = duration;
            this.recordConsumer = recordConsumer;
            this.timeout = timeout;
            this.partitionAssignmentListener = partitionAssignmentListener;
            this.partitionRevocationListener = partitionRevocationListener;
            this.commitStrategyFactory = commitStrategy;
            this.props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            this.queue = queue;
            this.keyFilter = r -> keyFilter.test(r.key());
        }

        public CompletableFuture<Map<TopicPartition, Offsets>> start() {
            CompletableFuture<Map<TopicPartition, Offsets>> f = new CompletableFuture<>();
            OffsetContextMap offsetCtx = new OffsetContextMap(f);
            isRunning.set(true);
            consumer = new KafkaConsumer(props, keyDeserializer, valueDeserializer);
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
            queue.offer(this);
        }

        public void run() {
            long processed;
            do {
                processed = _Callable.tryCatchIgnore(() ->
                        StreamSupport.stream(consumer.poll(duration).records(topic).spliterator(), false)
                                .filter(keyFilter)
                                .map(peek(r ->
                                        tryCatchIgnore(() -> recordConsumer.accept(r))
                                )).count(), -1L);
                if(processed > 0) {
                    commitStrategy.tryCommit();
                }
            } while(!this.yield(processed));
            if(!isRunning.get()) {
                consumer.close(timeout);
            } else {
                queue.offer(this);
            }
        }

        protected boolean yield(long processed) {
            return processed == 0;
        }

        public void stop() {
            isRunning.set(false);
        }
    }
}
