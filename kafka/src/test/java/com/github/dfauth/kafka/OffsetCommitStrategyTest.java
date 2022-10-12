package com.github.dfauth.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.dfauth.functional.Maps.extendedMap;
import static com.github.dfauth.kafka.RebalanceListener.seekToBeginning;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
            CompletableFuture<Map<TopicPartition, Long>> value = runner.runAsyncTest(f -> config -> {

                CompletableFuture<Map<TopicPartition, Long>> fut = new CompletableFuture<>();
                StreamBuilder.stringBuilder()
                        .withProperties(config, ConsumerConfig.GROUP_ID_CONFIG, "blah1")
                        .withTopic(TOPIC)
                        .withValueConsumer(v -> {})
                        .onPartitionAssignment(RebalanceListener.<String,String>currentOffsets(fut).andThen(seekToBeginning()))
                        .withOffsetCommitListener(c -> m -> fut.thenAccept(_m -> {
                            Map<TopicPartition, Long> lagMap = extendedMap(c.endOffsets(m.keySet())).map((tp, o) -> o - _m.get(tp));
                                log.info("lag is {}",lagMap);
                                f.complete(new HashMap<>(lagMap));
                        }))
                        .build()
                        .start();

                KafkaSink<String, String> sink = KafkaSink.<String>newStringKeyBuilder()
                        .withProperties(config)
                        .withTopic(TOPIC)
                        .withValueSerializer(new StringSerializer())
                        .build();
                assertNotNull(sink.publish(K, V).get(1000, TimeUnit.MILLISECONDS));
            });
            assertEquals(0L, (long)value.get(1000, TimeUnit.MILLISECONDS).get(new TopicPartition(TOPIC, 0)));
        }
    }
}
