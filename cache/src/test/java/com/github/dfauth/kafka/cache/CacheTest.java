package com.github.dfauth.kafka.cache;

import com.github.dfauth.functional.Lists;
import com.github.dfauth.kafka.EmbeddedKafka;
import com.github.dfauth.kafka.KafkaSink;
import com.github.dfauth.kafka.assertion.Assertions;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.github.dfauth.kafka.EmbeddedKafka.withEmbeddedKafka;
import static com.github.dfauth.kafka.RebalanceListener.seekToBeginning;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CacheTest {

    public static final String TOPIC = "topic";
    public static final String K = "key";
    public static final String V = "value";
    public static final String V1 = "value1";
    private static final int PARTITIONS = 1;

    @Test
    public void testIt() throws ExecutionException, InterruptedException {
        EmbeddedKafka.EmbeddedKafkaRunner runner = withEmbeddedKafka();
        try(runner) {
            CompletableFuture<String> value = runner
                    .withPartitions(PARTITIONS)
                    .withGroupId("blah")
                    .runAsyncTest(f -> config -> {
                        KafkaCache<String, String, String, String, String> cache = KafkaCache.stringBuilder()
                                .withProperties(config)
                                .withTopic(TOPIC)
                                .onPartitionAssignment(seekToBeginning())
                                .onMessage((k,v) -> f.complete(v))
                                .build();

                        cache.start(f);

                        KafkaSink<String, String> sink = KafkaSink.newStringBuilder()
                                .withProperties(config)
                                .withTopic(TOPIC)
                                .build();
                        RecordMetadata m = sink.publish(K, V).get(1000, TimeUnit.MILLISECONDS);
                        assertNotNull(m);
                    });
            assertEquals(V, value.get());
        }
    }

    @Test
    public void testCollection() throws ExecutionException, InterruptedException {
        EmbeddedKafka.EmbeddedKafkaRunner runner = withEmbeddedKafka();
        try(runner) {
            CompletableFuture<Assertions> value = runner
                    .withPartitions(PARTITIONS)
                    .withGroupId("blah")
                    .runAsyncTest(f -> config -> {
                        Assertions.Builder assertionBuilder = Assertions.builder();
                        Assertions.AssertionCallback<List<String>> f1 = assertionBuilder.assertThat(a1 -> assertEquals(List.of(V), a1), a2 -> assertEquals(List.of(V, V1), a2));
                        assertionBuilder.build(f);
                        KafkaCache<String, String, String, String, List<String>> cache = KafkaCache.<String, String, List<String>>unmappedStringKeyBuilder()
                                .withValueDeserializer(new StringDeserializer())
                                .withProperties(config)
                                .withTopic(TOPIC)
                                .onPartitionAssignment(seekToBeginning())
                                .onCacheMiss(k -> new ArrayList<>())
                                .computeIfPresent((oldV, newV) -> f1.assertValue(Lists.concat(oldV, newV)))
                                .build();

                        cache.start();

                        KafkaSink<String, String> sink = KafkaSink.newStringBuilder()
                                .withProperties(config)
                                .withTopic(TOPIC)
                                .build();
                        assertNotNull(sink.publish(K, V).get(1000, TimeUnit.MILLISECONDS));
                        assertNotNull(sink.publish(K, V1).get(1000, TimeUnit.MILLISECONDS));
                    });
            value.get().performAssertions();
        }
    }

}
