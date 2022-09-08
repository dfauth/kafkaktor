package com.github.dfauth.avro;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public abstract class EncryptingSerde<T> implements Serde<T> {

    private final Serde<T> nested;

    public EncryptingSerde(Serde<T> nested) {
        this.nested = nested;
    }

    @Override
    public Serializer<T> serializer() {
        return (t,o) -> encrypt(nested.serializer().serialize(t,o));
    }

    protected abstract byte[] encrypt(byte[] bytes);

    protected abstract byte[] decrypt(byte[] bytes);

    @Override
    public Deserializer<T> deserializer() {
        return (t,b) -> nested.deserializer().deserialize(t,decrypt(b));
    }
}
