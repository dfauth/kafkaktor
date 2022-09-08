package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.test.TestRequest;
import com.github.dfauth.avro.Envelope;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DispatchableTestActor extends DispatchableAktor {

    public DispatchableTestActor(KafkaAktorContext ctx) {
        super(ctx);
    }

    @Override
    public void handleTestRequest(Envelope<TestRequest, AktorMessageContext> envelope) {
        log.info("WOOZ ctx: {} testRequest: {}",envelope.messageContext(),envelope.payload());
        envelope.messageContext().sender().tell(envelope.payload().respond());
    }
}
