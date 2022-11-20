package com.github.dfauth.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.github.dfauth.functional.Maps.extendedMap;
import static com.github.dfauth.kafka.OffsetCommitStrategyTest.RecoveryState.initial;
import static com.github.dfauth.kafka.RebalanceListener.seekToBeginning;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class OffsetCommitStrategyTest {

    private static final String TOPIC = "topic";
    private static final String K = "k";
    private static final String V = "v";
    private static final int PARTITIONS = 1;

    @Test
    public void testIt() throws ExecutionException, InterruptedException, TimeoutException {
        EmbeddedKafka.EmbeddedKafkaRunner runner = EmbeddedKafka.embeddedKafkaWithTopics(TOPIC).withPartitions(PARTITIONS);

        try(runner) {
            CompletableFuture<OffsetMonitor> value = runner.runAsyncTest(f -> config -> {

                OffsetMonitor offsetMonitor = new OffsetMonitor(e -> log.info("WOOZ {}",e));

                StreamBuilder.stringBuilder()
                        .withProperties(config, ConsumerConfig.GROUP_ID_CONFIG, "blah1")
                        .withTopic(TOPIC)
                        .withValueConsumer(v -> {})
                        .onPartitionAssignment(RebalanceListener.<String,String>currentOffsets(offsetMonitor).andThen(seekToBeginning()))
                        .withOffsetCommitListener(c -> m -> {
                            offsetMonitor.reconcile(m);
                            f.complete(offsetMonitor);
                        })
                        .build()
                        .start(f);

                KafkaSink<String, String> sink = KafkaSink.<String>newStringKeyBuilder()
                        .withProperties(config)
                        .withTopic(TOPIC)
                        .withValueSerializer(new StringSerializer())
                        .build();
                assertNotNull(sink.publish(K, V).get(1000, TimeUnit.MILLISECONDS));
            });
            assertTrue(value.get(1000, TimeUnit.MILLISECONDS).getState(new TopicPartition(TOPIC, 0)).isRecovered());
        }
    }

    static class OffsetMonitor implements Consumer<Map<TopicPartition, Long>> {

        private Map<TopicPartition, RecoveryState> currentOffsets;
        private Consumer<RecoveryEvent> consumer;

        private OffsetMonitor(Consumer<RecoveryEvent> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void accept(Map<TopicPartition, Long> offsets) {
            this.currentOffsets = extendedMap(offsets).map((tp, o) -> initial(tp, o, consumer));
        }

        public void reconcile(Map<TopicPartition, OffsetAndMetadata> m) {
            extendedMap(m).map((tp, om) -> currentOffsets.compute(tp, (k, state) -> state.update(k,om)));
        }

        public RecoveryState getState(TopicPartition tp) {
            return currentOffsets.get(tp);
        }
    }

    static class RecoveryEvent {

        private final long offset;
        private final TopicPartition tp;

        public RecoveryEvent(TopicPartition tp, long offset) {
            this.tp = tp;
            this.offset = offset;
        }

        @Override
        public String toString() {
            return String.format("%s[%s,%d]",RecoveryEvent.class.getSimpleName(),tp,offset);
        }
    }

    interface RecoveryState {

        static RecoveryState initial(TopicPartition tp, long offset, Consumer<RecoveryEvent> consumer) {
            return new InitialRecoveryState(tp, offset, consumer);
        }

        long offset();

        RecoveryState recovered(TopicPartition tp, long offset);

        RecoveryState recovering(TopicPartition tp, long offset);

        default RecoveryState update(TopicPartition tp, OffsetAndMetadata om) {
            return Optional.ofNullable(om)
                    .map(OffsetAndMetadata::offset)
                    .filter(_o -> _o >= offset()-1)
                    .map(_o -> recovered(tp, _o))
                    .orElse(recovering(tp, offset()));
        }

        default boolean isRecovered() {
            return false;
        }
    }

    static class InitialRecoveryState implements RecoveryState {

        protected final TopicPartition tp;
        protected final long offset;
        protected final Consumer<RecoveryEvent> consumer;

        public InitialRecoveryState(TopicPartition tp, long offset, Consumer<RecoveryEvent> consumer) {
            this.tp = tp;
            this.offset = offset;
            this.consumer = consumer;
        }

        @Override
        public long offset() {
            return offset;
        }

        public RecoveryState recovered(TopicPartition tp, long offset) {
            return new RecoveredState(tp, offset, consumer);
        }

        public RecoveryState recovering(TopicPartition tp, long offset) {
            return new RecoveringState(tp, offset, consumer);
        }

    }

    static class RecoveredState extends InitialRecoveryState {

        public RecoveredState(TopicPartition tp, long offset, Consumer<RecoveryEvent> consumer) {
            super(tp, offset, consumer);
            consumer.accept(new RecoveryEvent(tp,offset));
        }

        @Override
        public RecoveryState recovered(TopicPartition tp, long offset) {
            return this;
        }

        public boolean isRecovered() {
            return true;
        }
    }

    static class RecoveringState extends InitialRecoveryState {

        public RecoveringState(TopicPartition tp, long offset, Consumer<RecoveryEvent> consumer) {
            super(tp, offset,consumer);
        }

        @Override
        public RecoveryState recovering(TopicPartition tp, long offset) {
            return this;
        }
    }
}
