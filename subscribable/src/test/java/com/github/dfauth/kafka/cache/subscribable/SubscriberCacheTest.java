package com.github.dfauth.kafka.cache.subscribable;

import com.github.dfauth.kafka.EmbeddedKafka;
import com.github.dfauth.kafka.KafkaSink;
import com.github.dfauth.kafka.RebalanceProcessor;
import com.github.dfauth.kafka.assertion.Assertions;
import com.github.dfauth.kafka.cache.KafkaCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.github.dfauth.functional.Functions.peek;
import static com.github.dfauth.kafka.EmbeddedKafka.withEmbeddedKafka;
import static com.github.dfauth.kafka.RebalanceListener.seekToBeginning;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class SubscriberCacheTest {

    public static final String TOPIC = "topic";
    public static final String K = "key";
    public static final String V = "value";
    public static final String V1 = "value1";
    private static final int PARTITIONS = 1;

    @Test
    public void testSubscriberCache() throws ExecutionException, InterruptedException {
        EmbeddedKafka.EmbeddedKafkaRunner runner = withEmbeddedKafka();
        try(runner) {
            CompletableFuture<Assertions> value = runner
                    .withPartitions(PARTITIONS)
                    .withGroupId("blah")
                    .runAsyncTest(f -> config -> {
                        CountDownLatch latch = new CountDownLatch(1);
                        Assertions.Builder assertionBuilder = Assertions.builder();
                        Assertions.AssertionCallback<List<String>> assertionCallback = assertionBuilder
                                .assertThat(
                                        o1 -> {
                                            log.info("WOOZ0 : "+o1);
                                            assertEquals(2, o1.size());
                                        },
                                        o2 -> assertEquals(2, o2.size()),
                                        o3 -> assertEquals(1, o3.size())
                                );
                        assertionBuilder.build(f);
                        KafkaCache<String, String, String, String, CachingPublisher<String>> cache = KafkaCache.<String, String, CachingPublisher<String>>unmappedStringKeyBuilder()
                                .withValueDeserializer(new StringDeserializer())
                                .withProperties(config)
                                .withTopic(TOPIC)
                                .onPartitionAssignment(seekToBeginning())
                                .onCacheMiss(k -> new CachingPublisher<>())
                                .computeIfPresent((p, v) -> {
                                    p.publish(v);
                                    return p;
                                })
                                .withRecoveryListener((tp,o) -> latch.countDown())
                                .withHighwatermark(RebalanceProcessor.endOffsets())
                                .build();

                        cache.start(f);

                        CountDownLatch latch1 = new CountDownLatch(2);
                        List<String> out1 = new ArrayList<>();
                        cache.get(K).ifPresent(p -> Flux.from(p)
                                .log("WOOZ1")
                                .map(peek(s -> latch1.countDown()))
                                .subscribe(out1::add));

                        CountDownLatch latch2 = new CountDownLatch(2);
                        List<String> out2 = new ArrayList<>();
                        cache.get(K).ifPresent(p -> Flux.from(p)
                                .map(peek(s -> latch2.countDown()))
                                .subscribe(out2::add));

                        KafkaSink<String, String> sink = KafkaSink.newStringBuilder()
                                .withProperties(config)
                                .withTopic(TOPIC)
                                .build();
                        latch.await(1000, TimeUnit.MILLISECONDS);
                        assertNotNull(sink.publish(K, V).get(1000, TimeUnit.MILLISECONDS));
                        assertNotNull(sink.publish(K, V1).get(1000, TimeUnit.MILLISECONDS));

                        latch1.await(1000, TimeUnit.MILLISECONDS);
                        latch2.await(1000, TimeUnit.MILLISECONDS);

                        CountDownLatch latch3 = new CountDownLatch(1);
                        List<String> out3 = new ArrayList<>();
                        cache.get(K).ifPresent(p -> Flux.from(p)
                                .map(peek(s -> latch3.countDown()))
                                .subscribe(out3::add));

                        latch3.await(1000, TimeUnit.MILLISECONDS);

                        assertionCallback.assertValue(out1);
                        assertionCallback.assertValue(out2);
                        assertionCallback.assertValue(out3);

                    });
            value.get().performAssertions();
        }
    }

}
