package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.TestAvroSerde;
import com.github.dfauth.avro.test.TestRequest;
import com.github.dfauth.avro.test.TestResponse;
import com.github.dfauth.kafka.assertion.Assertions;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.dfauth.kafka.EmbeddedKafka.withEmbeddedKafka;
import static com.github.dfauth.kafkaktor.Aktor.onStartup;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;
import static org.junit.Assert.assertEquals;

@Slf4j
public class SupervisorTest {

    public static final Long K = 1L;
    public static final String V = "value";
    private static final int PARTITIONS = 1;

    @Test
    public void testIt() {

        tryCatch(() -> {
            TestAvroSerde serde = new TestAvroSerde();

            CompletableFuture<Assertions> value = withEmbeddedKafka()
                    .withPartitions(PARTITIONS)
                    .runAsyncTest(f -> config -> {
                        Assertions.Builder assertions = Assertions.builder();
                        CompletableFuture<TestResponse> f1 = assertions.assertThat(r -> assertEquals(TestResponse.newBuilder().setKey(K).setValue(V).build(),r));
                        assertions.build(f);
                        AktorSystem system = AktorSystem.create(config, serde);
                        Supervisor.create(system);

                        system.newAktor(onStartup(ctx -> {
                            CompletableFuture<AktorReference<TestRequest>> fRef = ctx.spawn("blah", TestActor.class);
                            fRef.thenAccept(ref ->
                                    ref.<TestResponse>ask(TestRequest.newBuilder().setKey(K).setValue(V).build())
                                            .thenAccept(r -> {
                                                log.info("WOOZ received reply: {} from {}",r,ref.key());
                                                f1.complete(r);
                                            }));
                        }));
                    });
            value.get(25000, TimeUnit.MILLISECONDS).performAssertions();
        });
    }
}
