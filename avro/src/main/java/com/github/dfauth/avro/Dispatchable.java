package com.github.dfauth.avro;

import org.apache.avro.specific.SpecificRecord;

public interface Dispatchable<T extends Dispatchable<T>> extends SpecificRecord {

    <R extends MessageContext> void dispatch(Envelope<T, R> e, EnvelopeHandler<R> envelopeHandler);
}
