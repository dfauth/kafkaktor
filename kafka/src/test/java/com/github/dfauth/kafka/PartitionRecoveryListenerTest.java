package com.github.dfauth.kafka;

import com.github.dfauth.functional.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.dfauth.functional.Tuple2.tuple2;
import static com.github.dfauth.kafka.RebalanceListener.seekToBeginning;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class PartitionRecoveryListenerTest {

    private static final String TOPIC = "topic";
    private static final String K = "k";
    private static final String V = "v";
    private static final int PARTITIONS = 1;

    @Test
    public void testIt() throws ExecutionException, InterruptedException, TimeoutException {
        EmbeddedKafka.EmbeddedKafkaRunner runner = EmbeddedKafka.embeddedKafkaWithTopics(TOPIC).withPartitions(PARTITIONS);

        try(runner) {
            CompletableFuture<Tuple2<TopicPartition, Long>> value = runner.runAsyncTest(f -> config -> {

                StreamBuilder.stringBuilder()
                        .withProperties(config, ConsumerConfig.GROUP_ID_CONFIG, "blah1")
                        .withTopic(TOPIC)
                        .onPartitionAssignment(seekToBeginning())
                        .withValueConsumer(v -> {})
                        .withRecoveryListener((tp,offset) -> {
                            f.complete(tuple2(tp,offset));
                        })
                        .build()
                        .start(f);

                KafkaSink<String, String> sink = KafkaSink.<String>newStringKeyBuilder()
                        .withProperties(config)
                        .withTopic(TOPIC)
                        .withValueSerializer(new StringSerializer())
                        .build();
                assertNotNull(sink.publish(K, V).get(1000, TimeUnit.MILLISECONDS));
                sleep(10000);
            });
            assertEquals(tuple2(new TopicPartition(TOPIC, 0),0L), value.get(1000, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testMultiplePartitions() throws ExecutionException, InterruptedException, TimeoutException {
        EmbeddedKafka.EmbeddedKafkaRunner runner = EmbeddedKafka.embeddedKafkaWithTopics(TOPIC).withPartitions(2);

        try(runner) {
            CompletableFuture<List<TopicPartition>> value = runner.runAsyncTest(f -> config -> {

                List<TopicPartition> partitions = new ArrayList<>();
                StreamBuilder.stringBuilder()
                        .withProperties(config, ConsumerConfig.GROUP_ID_CONFIG, "blah1")
                        .withTopic(TOPIC)
                        .withValueConsumer(v -> {})
                        .onPartitionAssignment(seekToBeginning())
                        .withRecoveryListener((tp,offset) -> {
                            partitions.remove(tp);
                            if(partitions.isEmpty()) {
                                f.complete(partitions);
                            }
                        })
                        .build()
                        .start(f);

                KafkaSink<String, String> sink = KafkaSink.<String>newStringKeyBuilder()
                        .withProperties(config)
                        .withTopic(TOPIC)
                        .withValueSerializer(new StringSerializer())
                        .build();
                assertNotNull(sink.publish(null, V).get(1000, TimeUnit.MILLISECONDS));
                assertNotNull(sink.publish(null, V).get(1000, TimeUnit.MILLISECONDS));
                sleep(10000);
            });
            assertEquals(Collections.emptyList(), value.get(1000, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testDefault() throws ExecutionException, InterruptedException, TimeoutException {
        EmbeddedKafka.EmbeddedKafkaRunner runner = EmbeddedKafka.embeddedKafkaWithTopics(TOPIC).withPartitions(2);

        try(runner) {
            CompletableFuture<Map<TopicPartition, Long>> value = runner.runAsyncTest(f -> config -> {

                RecoveryListener<String,String> recoveryListener = new RecoveryListener<>();
                StreamBuilder.stringBuilder()
                        .withProperties(config, ConsumerConfig.GROUP_ID_CONFIG, "blah1")
                        .withTopic(TOPIC)
                        .withValueConsumer(v -> {})
                        .onPartitionAssignment(seekToBeginning())
                        .withRecoveryListener(recoveryListener.onRecovery(f::complete))
                        .build()
                        .start(f);

                KafkaSink<String, String> sink = KafkaSink.<String>newStringKeyBuilder()
                        .withProperties(config)
                        .withTopic(TOPIC)
                        .withValueSerializer(new StringSerializer())
                        .build();
                assertNotNull(sink.publish(null, V).get(1000, TimeUnit.MILLISECONDS));
                assertNotNull(sink.publish(null, V).get(1000, TimeUnit.MILLISECONDS));
                sleep(10000);
            });
            assertEquals(Map.of(new TopicPartition(TOPIC,0),0L,new TopicPartition(TOPIC,1),0L), value.get(1000, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testNoMessages() throws ExecutionException, InterruptedException, TimeoutException {
        EmbeddedKafka.EmbeddedKafkaRunner runner = EmbeddedKafka.embeddedKafkaWithTopics(TOPIC).withPartitions(2);

        try(runner) {
            CompletableFuture<Map<TopicPartition, Long>> value = runner.runAsyncTest(f -> config -> {

                RecoveryListener<String, String> recoveryListener = new RecoveryListener<>();
                StreamBuilder.stringBuilder()
                        .withProperties(config, ConsumerConfig.GROUP_ID_CONFIG, "blah1")
                        .withTopic(TOPIC)
                        .withValueConsumer(v -> {})
                        .onPartitionAssignment(seekToBeginning())
                        .withRecoveryListener(recoveryListener.onRecovery(f::complete))
                        .build()
                        .start(f);
                sleep(10000);
            });
            assertEquals(Map.of(new TopicPartition(TOPIC,0),0L,new TopicPartition(TOPIC,1),0L), value.get(1000, TimeUnit.MILLISECONDS));
        }
    }
}
