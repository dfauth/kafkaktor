package com.github.dfauth.kafka.cache;

import com.github.dfauth.kafka.KafkaSink;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Test;

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
    private static final int PARTITIONS = 1;

    @Test
    public void testIt() throws ExecutionException, InterruptedException {
        CompletableFuture<String> value = withEmbeddedKafka()
                .withPartitions(PARTITIONS)
                .withGroupId("blah")
                .runAsyncTest(f -> config -> {
                    KafkaCache<String, String, String, String> cache = KafkaCache.stringBuilder()
                            .withProperties(config)
                            .withTopic(TOPIC)
                            .onPartitionAssignment(seekToBeginning())
                            .onMessage((k,v) -> f.complete(v))
                            .build();

                    cache.start();

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
