package com.github.dfauth.kafka.utils;

import com.github.dfauth.avro.Envelope;
import com.github.dfauth.avro.MessageContext;
import com.github.dfauth.functional.Tuple2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrimitiveHeader implements Header {

    private final String key;
    private final byte[] value;

    public PrimitiveHeader(String key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    public static PrimitiveHeader fromHeader(Header header) {
        return new PrimitiveHeader(header.key(), header.value());
    }

    public static Map<String, Object> fromHeaders(Headers headers) {
        return Stream.of(headers.toArray())
                .map(PrimitiveHeader::fromHeader)
                .map(h -> h.rehydrate().toMapEntry())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Iterable<Header> create(Map<String, Object> metadata) {
        return metadata.entrySet().stream().map(e -> create(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    public static PrimitiveHeader create(String key, Object value) {
        return PrimitiveEncoding.trySerialize(value).map(bytes -> new PrimitiveHeader(key, bytes)).orElseThrow(() -> new IllegalArgumentException("Unsupported header values type: "+value));
    }

    public static <K,V> Consumer<ConsumerRecord<K, V>> toConsumerRecordConsumer(BiConsumer<K, Envelope<V,MessageContext>> consumer) {
        return r -> consumer.accept(r.key(), Envelope.withMetadata(fromHeaders(r.headers()), r.value()));
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public byte[] value() {
        return value;
    }

    public Tuple2<String, Object> rehydrate() {
        Object v = PrimitiveEncoding.tryDeserialize(value).orElse(value);
        return Tuple2.tuple2(key, v);
    }
}
