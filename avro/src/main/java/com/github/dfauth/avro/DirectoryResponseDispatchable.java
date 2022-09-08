package com.github.dfauth.avro;

import com.github.dfauth.avro.actor.DirectoryResponse;

public interface DirectoryResponseDispatchable extends Dispatchable<DirectoryResponse> {

    default <R extends MessageContext> void dispatch(Envelope<DirectoryResponse,R> e, EnvelopeHandler<R> envelopeHandler) {
        envelopeHandler.handleDirectoryResponse(e);
    }

}
