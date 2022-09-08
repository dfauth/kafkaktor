package com.github.dfauth.avro;

import com.github.dfauth.avro.actor.ActorCreationRequest;

public interface ActorCreationRequestDispatchable extends Dispatchable<ActorCreationRequest> {

    static ActorCreationRequest newRequest(Class<?> actorClass) {
        return newRequest(actorClass.getCanonicalName(), actorClass);
    }

    static ActorCreationRequest newRequest(String name, Class<?> actorClass) {
        return ActorCreationRequest.newBuilder().setName(name).setClassName(actorClass.getCanonicalName()).build();
    }

    @Override
    default <R extends MessageContext> void dispatch(Envelope<ActorCreationRequest, R> e, EnvelopeHandler<R> envelopeHandler) {
        envelopeHandler.handleActorCreationRequest(e);
    }
}
