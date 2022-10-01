package com.github.dfauth.kafka;

import com.github.dfauth.kafka.assertion.Assertions;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.dfauth.kafka.utils.PrimitiveHeader.toConsumerRecordConsumer;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class YieldTest {

    public static final String TOPIC = "topic";
    public static final String K1 = "key1";
    public static final String V1 = "value1";
    public static final Map<String, Object> H1 = Map.of("k1", "v", "k2", 1, "k3", false, "k4", 0.0d, "k5", 3l, "k6", 4.14f);
    public static final String K2 = "key2";
    public static final String V2 = "value2";
    public static final Map<String, Object> H2 = Map.of("k1", "v", "k2", 2, "k3", false, "k4", 0.0d, "k5", 3l, "k6", 4.14f);
    public static final String K3 = "key3";
    public static final String V3 = "value3";
    public static final Map<String, Object> H3 = Map.of("k1", "v", "k2", 3, "k3", false, "k4", 0.0d, "k5", 3l, "k6", 4.14f);
    private static final int PARTITIONS = 1;

    @Test
    public void testIt() throws Exception {

        CompletableFuture<Assertions> value = EmbeddedKafka.embeddedKafkaWithTopics(TOPIC)
                .withPartitions(PARTITIONS)
                .runAsyncTest(f -> config -> {
                    Assertions.Builder assertions = Assertions.builder();
                    Assertions.OptionalQueue<CompletableFuture<String>> f0 = assertions.assertThat(k -> assertEquals(K1, k), k -> assertEquals(K2, k), k -> assertEquals(K3, k));
                    Assertions.OptionalQueue<CompletableFuture<Map<String, Object>>> f1 = assertions.assertThat(h1 -> assertEquals(H1, h1),h2 -> assertEquals(H2, h2),h3 -> assertEquals(H3, h3));
                    Assertions.OptionalQueue<CompletableFuture<String>> f2 = assertions.assertThat(v -> assertEquals(V1, v), v -> assertEquals(V2, v),v -> assertEquals(V3, v));
                    Assertions.OptionalQueue<CompletableFuture<String>> f4 = assertions.assertThat(k -> assertEquals(K1, k), k -> assertEquals(K2, k),k -> assertEquals(K3, k));
                    Assertions.OptionalQueue<CompletableFuture<Map<String, Object>>> f5 = assertions.assertThat(h1 -> assertEquals(H1, h1),h2 -> assertEquals(H2, h2),h3 -> assertEquals(H3, h3));
                    Assertions.OptionalQueue<CompletableFuture<String>> f6 = assertions.assertThat(v -> assertEquals(V1, v), v -> assertEquals(V2, v),v -> assertEquals(V3, v));
                    assertions.build(f);

                    StreamBuilder<String, String, String, String> builder = StreamBuilder.<String>stringKeyUnmappedValueBuilder()
                            .withValueDeserializer(new StringDeserializer())
                            .withProperties(config, ConsumerConfig.GROUP_ID_CONFIG, "blah1")
                            .withTopic(TOPIC)
                            .withRecordConsumer(toConsumerRecordConsumer((k, e) -> {
                                    f0.poll().ifPresent(_f -> _f.complete(k));
                                    f1.poll().ifPresent(_f -> _f.complete(e.messageContext().metadata()));
                                    f2.poll().ifPresent(_f -> _f.complete(e.payload()));
                                }))
                            .onPartitionAssignment(RebalanceListener.seekToBeginning());
                    builder.build().start();

                    builder.withProperties(config, ConsumerConfig.GROUP_ID_CONFIG, "blah2")
                            .withRecordConsumer(toConsumerRecordConsumer((k,e) -> {
                                f4.poll().ifPresent(_f -> _f.complete(k));
                                f5.poll().ifPresent(_f -> _f.complete(e.messageContext().metadata()));
                                f6.poll().ifPresent(_f -> _f.complete(e.payload()));
                            }))
                            .onPartitionAssignment(RebalanceListener.seekToBeginning())
                            .build().start();

                    KafkaSink<String, String> sink = KafkaSink.<String>newStringKeyBuilder()
                            .withProperties(config)
                            .withTopic(TOPIC)
                            .withValueSerializer(new StringSerializer())
                            .build();
                    assertNotNull(sink.publish(K1, V1, H1).get(1000, TimeUnit.MILLISECONDS));
                    sleep(1000);
                    assertNotNull(sink.publish(K2, V2, H2).get(1000, TimeUnit.MILLISECONDS));
                    sleep(1000);
                    assertNotNull(sink.publish(K3, V3, H3).get(1000, TimeUnit.MILLISECONDS));
                });
        value.get(10000, TimeUnit.MILLISECONDS).performAssertions();
    }
}
