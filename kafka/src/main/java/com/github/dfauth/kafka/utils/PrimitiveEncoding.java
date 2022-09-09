package com.github.dfauth.kafka.utils;

import com.github.dfauth.functional.Try;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public enum PrimitiveEncoding {

    STRING_ENCODING(
            v -> ((String) v).getBytes(),
            String::new
            ),
    INT_ENCODING(
            v -> ByteBuffer.allocate(4).putInt((int)v).array(),
            b -> ByteBuffer.wrap(b).getInt()
            ),
    SHORT_ENCODING(
            v -> ByteBuffer.allocate(2).putShort((short)v).array(),
            b -> ByteBuffer.wrap(b).getShort()
            ),
    CHAR_ENCODING(
            v -> ByteBuffer.allocate(4).putChar((char)v).array(),
            b -> ByteBuffer.wrap(b).getChar()
            ),
    BOOL_ENCODING(
            v -> (Boolean) v ? new byte[]{1} : new byte[]{0},
            b -> ByteBuffer.wrap(new byte[]{0,b[0]}).getShort() == 1
            ),
    DOUBLE_ENCODING(
            v -> ByteBuffer.allocate(8).putDouble((double)v).array(),
            b -> ByteBuffer.wrap(b).getDouble()
    ),
    FLOAT_ENCODING(
            v -> ByteBuffer.allocate(8).putFloat((float)v).array(),
            b -> ByteBuffer.wrap(b).getFloat()
    ),
    LONG_ENCODING(
            v -> ByteBuffer.allocate(8).putLong((long)v).array(),
            b -> ByteBuffer.wrap(b).getLong()
            );

    public static final byte MAGIC = 0x69;

    private final Function<Object, byte[]> f;
    private final Function<byte[], Object> g;

    PrimitiveEncoding(Function<Object, byte[]> f, Function<byte[], Object> g) {
        this.f = f;
        this.g = g;
    }

    public static Optional<byte[]> trySerialize(Object o) {
        return Stream.of(values()).map(v ->
                Try.trySilentlyWithCallable(() -> v.serialize(o))
        ).filter(Try::isSuccess).findFirst().flatMap(Try::toOptional);
    }

    public static Optional<Object> tryDeserialize(byte[] b) {
        return Stream.of(values()).map(v ->
                Try.trySilentlyWithCallable(() -> v.deserialize(b))
        ).filter(Try::isSuccess).findFirst().flatMap(Try::toOptional);
    }

    public byte[] serialize(Object o) {
        byte ordByte = ByteBuffer.allocate(4).putInt(ordinal()).array()[3];
        byte[] payloadBytes = f.apply(o);
        byte[] result = new byte[payloadBytes.length + 2];
        result[0] = MAGIC;
        result[1] = ordByte;
        System.arraycopy(payloadBytes, 0, result, 2, payloadBytes.length);
        return result;
    }

    public Object deserialize(byte[] b) {
        if(b[0] == MAGIC) {
            return PrimitiveEncoding.values()[ByteBuffer.wrap(new byte[]{0,0,0,b[1]}).getInt()].g.apply(Arrays.copyOfRange(b, 2, b.length));
        }
        throw new IllegalArgumentException("Oops");
    }
}
