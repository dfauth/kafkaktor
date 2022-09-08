package com.github.dfauth.avro;

import com.github.dfauth.avro.actor.DirectoryRequest;

public interface DirectoryRequestDispatchable extends Dispatchable<DirectoryRequest> {

    static DirectoryRequest newRequest(String name) {
        return DirectoryRequest.newBuilder().setName(name).build();
    }

    default <R extends MessageContext> void dispatch(Envelope<DirectoryRequest, R> e, EnvelopeHandler<R> envelopeHandler) {
        envelopeHandler.handleDirectoryRequest(e);
    }
}
