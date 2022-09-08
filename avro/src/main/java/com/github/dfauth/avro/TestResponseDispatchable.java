package com.github.dfauth.avro;

import com.github.dfauth.avro.test.TestResponse;

public interface TestResponseDispatchable extends Dispatchable<TestResponse> {

    default <R extends MessageContext> void dispatch(Envelope<TestResponse,R> e, EnvelopeHandler<R> envelopeHandler) {
        envelopeHandler.handleTestResponse(e);
    }
}
