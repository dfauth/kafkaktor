package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.test.TestRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class TestActor implements Aktor<TestRequest> {

    private final KafkaAktorContext ktx;

    public TestActor(KafkaAktorContext ktx) {
        this.ktx = ktx;
    }

    @Override
    public MessageContextAware<Consumer<TestRequest>> withAktorContext(AktorContext ktx) {
        return m ->
                p -> {
                    log.info("WOOZ ktx: {} mktx: {} payload: {}",ktx,m,p);
                    m.sender().tell(p.respond());
                };
    }

    @Override
    public CompletableFuture<AktorAddress> start() {
        return ktx.address();
    }
}
