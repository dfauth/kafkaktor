package com.github.dfauth.kafka.cache;

import com.github.dfauth.kafka.KafkaSink;
import com.github.dfauth.kafka.RebalanceListener;
import com.github.dfauth.kafka.assertion.Assertions;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.github.dfauth.kafka.EmbeddedKafka.withEmbeddedKafka;
import static com.github.dfauth.kafka.RebalanceListener.seekToBeginning;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class MultiConsumerCacheTest {

    public static final String TOPIC = "topic";
    public static final String V1 = "value1";
    public static final String V6 = "value6";
    public static final String V2 = "value2";
    public static final String V3 = "value3";
    public static final String V4 = "value4";
    public static final String V5 = "value5";
    private static final int PARTITIONS = 6;

    @Test
    public void testIt() throws ExecutionException, InterruptedException {
        CompletableFuture<Assertions> value = withEmbeddedKafka()
                .withPartitions(PARTITIONS)
                .withGroupId("blah")
                .runAsyncTest(f -> config -> {
                    Assertions.Builder assertionBuilder = Assertions.builder();
                    Assertions.AssertionCallback<String> assertionCallback = assertionBuilder
                            .assertThat(
                                    v1 -> assertEquals(V1, v1),
                                    v2 -> assertEquals(V2, v2),
                                    v3 -> assertEquals(V3, v3),
                                    v4 -> assertEquals(V4, v4),
                                    v5 -> assertEquals(V5, v5),
                                    v6 -> assertEquals(V6, v6)
                            );
                    assertionBuilder.build(f);
                    KafkaCache<String, String, String, String, String> cache = KafkaCache.stringBuilder()
                            .withProperties(config)
                            .withTopic(TOPIC)
                            .withConsumersNumbering(6)
                            .onPartitionAssignment(MultiConsumerCacheTest.<String,String>logPartitionAssignment("WOOZ").compose(seekToBeginning()))
                            .build();

                    cache.start();

                    KafkaSink<String, String> sink = KafkaSink.newStringBuilder()
                            .withProperties(config)
                            .withTopic(TOPIC)
                            .build();
                    assertNotNull(sink.publish("1", V1).get(1000, TimeUnit.MILLISECONDS));
                    assertNotNull(sink.publish("2", V2).get(1000, TimeUnit.MILLISECONDS));
                    assertNotNull(sink.publish("3", V3).get(1000, TimeUnit.MILLISECONDS));
                    assertNotNull(sink.publish("4", V4).get(1000, TimeUnit.MILLISECONDS));
                    assertNotNull(sink.publish("5", V5).get(1000, TimeUnit.MILLISECONDS));
                    assertNotNull(sink.publish("6", V6).get(1000, TimeUnit.MILLISECONDS));

                    Thread.sleep(1000);
                    assertionCallback.assertValue(cache.get("1").orElseThrow());
                    assertionCallback.assertValue(cache.get("2").orElseThrow());
                    assertionCallback.assertValue(cache.get("3").orElseThrow());
                    assertionCallback.assertValue(cache.get("4").orElseThrow());
                    assertionCallback.assertValue(cache.get("5").orElseThrow());
                    assertionCallback.assertValue(cache.get("6").orElseThrow());
                });
        value.get().performAssertions();
    }

    private static <K,V>RebalanceListener<K, V> logPartitionAssignment(String msg) {
        return c -> tps -> tps.forEach(tp -> log.info(msg+" assigned partition "+tp+" to consumer "+c));
    }

}
