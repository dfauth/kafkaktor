package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.ActorCreationRequestDispatchable;
import com.github.dfauth.avro.TestAvroSerde;
import com.github.dfauth.avro.actor.ActorCreationRequest;
import com.github.dfauth.kafka.EmbeddedKafka;
import com.github.dfauth.kafka.assertion.Assertions;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.dfauth.kafkaktor.AktorMessageContext.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class AktorTest {

    public static final String TOPIC = "genesis";
    public static final String K = "key";
    public static final ActorCreationRequest V = ActorCreationRequestDispatchable.newRequest(TestActor.class);
    public static final Map<String, Object> H = Map.of("k1", "v", "k2", 0, "k3", false, "k4", 0.0d, "k5", 3l, "k6", 4.14f);
    private static final int PARTITIONS = 1;

    @Test
    public void testIt() {

        try {
            TestAvroSerde serde = new TestAvroSerde();

            EmbeddedKafka.EmbeddedKafkaRunner runner = EmbeddedKafka.embeddedKafkaWithTopics(TOPIC);

            try(runner) {
                CompletableFuture<Assertions> value = runner
                        .withPartitions(PARTITIONS)
                        .withGroupId("blah")
                        .runAsyncTest(f -> config -> {
                            AktorSystem system = new AktorSystem(config, serde);
                            Assertions.Builder assertions = Assertions.builder();
                            CompletableFuture<AktorMessageContext> f1 = assertions.assertThat(v -> {
                                H.forEach((_k,_v) -> assertEquals(_v, v.metadata().get(_k)));
                                assertTrue(v.metadata().containsKey(SENDER_KEY));
                                assertTrue(v.metadata().containsKey(SENDER_TOPIC));
                                assertTrue(v.metadata().containsKey(SENDER_PARTITION));
                            });
                            CompletableFuture<ActorCreationRequest> f2 = assertions.assertThat(v -> assertEquals(V, v));
                            assertions.build(f);

                            CompletableFuture<AktorReference<ActorCreationRequest>> fRef = system.newAktor(ktx -> m -> p -> {
                                f1.complete(m);
                                f2.complete(p);
                            });

                            fRef.thenAccept(ref -> {
                                ref.tell(V, H);
                            });

                        });
                value.get(7000, TimeUnit.MILLISECONDS).performAssertions();
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
