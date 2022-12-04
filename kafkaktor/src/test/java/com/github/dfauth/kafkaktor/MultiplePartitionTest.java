package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.TestAvroSerde;
import com.github.dfauth.avro.test.TestRequest;
import com.github.dfauth.avro.test.TestResponse;
import com.github.dfauth.kafka.EmbeddedKafka;
import com.github.dfauth.kafka.assertion.Assertions;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.dfauth.kafka.EmbeddedKafka.withEmbeddedKafka;
import static com.github.dfauth.kafkaktor.Aktor.onStartup;
import static org.junit.Assert.assertEquals;

@Slf4j
public class MultiplePartitionTest {

    public static final Long K = 1L;
    public static final String V = "value";
    private static final int PARTITIONS = 1;

    @Ignore
    @Test
    public void testIt() throws Exception {

        TestAvroSerde serde = new TestAvroSerde();

        EmbeddedKafka.EmbeddedKafkaRunner runner = withEmbeddedKafka();

        try(runner) {
            CompletableFuture<Assertions> value = withEmbeddedKafka()
                    .withPartitions(PARTITIONS)
                    .runAsyncTest(f -> config -> {

                        TestRequest REF1 = TestRequest.newBuilder().setKey(K).setValue("REF1").build();
                        TestRequest REF2 = TestRequest.newBuilder().setKey(K).setValue("REF2").build();

                        Assertions.Builder assertions = Assertions.builder();
                        CompletableFuture<TestResponse> f1 = assertions.assertThat(r -> assertEquals(REF1.respond(),r));
//                    CompletableFuture<TestResponse> f2 = assertions.assertThat(r -> assertEquals(REF2,r.getValue()));
                        assertions.build(f);

                        blah(config, serde, f1, REF1);
//                    blah(config, avroSerialization, f2, REF2);

                    });
            value.get(10000, TimeUnit.MILLISECONDS).performAssertions();
        }
    }

    private void blah(Map<String, Object> config, TestAvroSerde serde, CompletableFuture<TestResponse> f, TestRequest req) {
        AktorSystem system = AktorSystem.builder().withConfig(config).withSerde(serde).build();

        AktorReference<SpecificRecord> fRef = system.newAktor(oneTimeAktor(ctx -> m -> p -> {}
//                m.sender().tell(p.respond())
        ));

        system.newAktor(onStartup(ctx ->
            fRef.<TestResponse>ask(req).thenAccept(f::complete)
        ));
    }

    private <T> AktorContextAware<T> oneTimeAktor(AktorContextAware<T> f) {
        return f;
    }

}
