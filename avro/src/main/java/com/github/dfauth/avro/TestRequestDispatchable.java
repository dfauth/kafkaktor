package com.github.dfauth.avro;

import com.github.dfauth.avro.test.TestRequest;
import com.github.dfauth.avro.test.TestResponse;

public interface TestRequestDispatchable extends Dispatchable<TestRequest> {

    default <R extends MessageContext> void dispatch(Envelope<TestRequest, R> e, EnvelopeHandler<R> envelopeHandler) {
        envelopeHandler.handleTestRequest(e);
    }

    default TestResponse respond() {
        return TestResponse.newBuilder().setKey(getKey()).setValue(getValue()).build();
    }

    long getKey();

    String getValue();
}
