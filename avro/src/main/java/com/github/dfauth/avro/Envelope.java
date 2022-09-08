package com.github.dfauth.avro;

import java.util.Map;
import java.util.function.Function;

public interface Envelope<T, R extends MessageContext> {

    static <T, R extends MessageContext> Envelope<T,R> asEnvelope(R ctx, T payload) {
        return new Envelope<>() {
            @Override
            public R messageContext() {
                return ctx;
            }

            @Override
            public T payload() {
                return payload;
            }
        };
    }

    static <T> Envelope<T,MessageContext> withMetadata(Map<String, Object> metadata, T payload) {
        return new Envelope<>() {

            @Override
            public MessageContext messageContext() {
                return () -> metadata;
            }

            @Override
            public T payload() {
                return payload;
            }
        };
    }

    R messageContext();

    T payload();

    default <S> Envelope<S, R> map(Function<T,S> f) {
        T p = payload();
        R m = messageContext();
        return new Envelope<>() {
            @Override
            public R messageContext() {
                return m;
            }

            @Override
            public S payload() {
                return f.apply(p);
            }
        };
    }
}
