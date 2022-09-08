package com.github.dfauth.avro;

import com.github.dfauth.avro.actor.ActorCreationRequest;
import com.github.dfauth.avro.actor.DirectoryRequest;
import com.github.dfauth.avro.actor.DirectoryResponse;
import com.github.dfauth.avro.test.TestRequest;
import com.github.dfauth.avro.test.TestResponse;

public interface EnvelopeHandler<R extends MessageContext> {

    default void handleDirectoryRequest(Envelope<DirectoryRequest,R> e) {
        handleOther(e.map(DirectoryRequest.class::cast));
    }

    default void handleDirectoryResponse(Envelope<DirectoryResponse,R> e) {
        handleOther(e.map(DirectoryResponse.class::cast));
    }

    default void handleActorCreationRequest(Envelope<ActorCreationRequest,R> e) {
        handleOther(e.map(ActorCreationRequest.class::cast));
    }

    default void handleTestRequest(Envelope<TestRequest,R> e) {
        handleOther(e.map(TestRequest.class::cast));
    }

    default void handleTestResponse(Envelope<TestResponse,R> e) {
        handleOther(e.map(TestResponse.class::cast));
    }

    default <T extends Dispatchable<T>> void handleOther(Envelope<T,R> e) {
    }

}
