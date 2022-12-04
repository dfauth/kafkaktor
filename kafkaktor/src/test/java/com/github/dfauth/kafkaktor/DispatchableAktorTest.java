package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.*;
import com.github.dfauth.avro.actor.ActorCreationRequest;
import com.github.dfauth.avro.actor.DirectoryRequest;
import com.github.dfauth.avro.test.TestRequest;
import com.github.dfauth.avro.test.TestResponse;
import com.github.dfauth.kafka.EmbeddedKafka;
import com.github.dfauth.kafka.assertion.Assertions;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.dfauth.kafka.EmbeddedKafka.withEmbeddedKafka;
import static com.github.dfauth.kafkaktor.Aktor.onStartup;
import static org.junit.Assert.assertEquals;

@Slf4j
public class DispatchableAktorTest {

    public static final Long K = 1L;
    public static final String V = "value";
    private static final int PARTITIONS = 1;

    @Ignore
    @Test
    public void testIt() throws Exception {

        TestAvroSerde serde = new TestAvroSerde();

        CompletableFuture<Assertions> value = withEmbeddedKafka()
                .withPartitions(PARTITIONS)
                .runAsyncTest(f -> config -> {
                    ActorCreationRequest REF1 = ActorCreationRequestDispatchable.newRequest(Supervisor.class);
                    DirectoryRequest REF2 = DirectoryRequestDispatchable.newRequest("fred");
                    TestRequest REF3 = TestRequest.newBuilder().setKey(K).setValue(V).build();
                    Assertions.Builder assertions = Assertions.builder();
                    CompletableFuture<ActorCreationRequest> f1 = assertions.assertThat(r -> assertEquals(REF1,r));
                    CompletableFuture<DirectoryRequest> f2 = assertions.assertThat(r -> assertEquals(REF2,r));
                    CompletableFuture<TestRequest> f3 = assertions.assertThat(r -> assertEquals(REF3,r));
                    assertions.build(f);
                    AktorSystem system = AktorSystem.builder().withConfig(config).withSerde(serde).build();

                    AktorReference<Dispatchable> fRef = system.newAktor(new DispatchableAktor(system) {

                        @Override
                        public void handleDirectoryRequest(Envelope<DirectoryRequest, AktorMessageContext> e) {
                            f2.complete(e.payload());
                        }

                        @Override
                        public void handleActorCreationRequest(Envelope<ActorCreationRequest, AktorMessageContext> e) {
                            f1.complete(e.payload());
                        }

                        @Override
                        public void handleTestRequest(Envelope<TestRequest, AktorMessageContext> e) {
                            f3.complete(e.payload());
                        }
                    });

                    system.newAktor(onStartup(ctx -> {
                        fRef.tell(REF1);
                        fRef.tell(REF2);
                        fRef.tell(REF3);
                    }));
                });
        value.get(10000, TimeUnit.MILLISECONDS).performAssertions();
    }

    @Ignore
    @Test
    public void testResponse() throws Exception {

        TestAvroSerde serde = new TestAvroSerde();

        EmbeddedKafka.EmbeddedKafkaRunner runner = withEmbeddedKafka();
        try(runner) {
            CompletableFuture<Assertions> value = runner
                    .withPartitions(PARTITIONS)
                    .runAsyncTest(f -> config -> {
                        Assertions.Builder assertions = Assertions.builder();
                        CompletableFuture<TestResponse> f1 = assertions.assertThat(r -> assertEquals(TestResponse.newBuilder().setKey(K).setValue(V).build(),r));
                        assertions.build(f);
                        AktorSystem system = AktorSystem.builder().withConfig(config).withSerde(serde).build();
                        Supervisor.create(system);

                        system.newAktor(onStartup(ctx -> {
                            CompletableFuture<AktorReference<Dispatchable>> fRef = ctx.spawn("despatchable", DispatchableTestActor.class);
                            fRef.thenApply(ref -> ref.<TestResponse>ask(TestRequest.newBuilder().setKey(K).setValue(V).build()).thenAccept(r -> {
                                log.info("WOOZ received reply: {} from {}",r,ref.key());
                                f1.complete(r);
                            }));
                        }));
                    });
            value.get(10000, TimeUnit.MILLISECONDS).performAssertions();
        }
    }
}
