package com.github.dfauth.kafka.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
public class KafkaUtils {

    public static void logErrors(CompletableFuture<RecordMetadata> f) {
        f.whenComplete((_t, e) -> Optional.ofNullable(e).ifPresent(_e -> log.error("WOOZ "+_e.getMessage(), e)));
    }

    public static void logMetadata(CompletableFuture<RecordMetadata> f) {
        f.whenComplete((_t, e) -> Optional.ofNullable(e).ifPresentOrElse(_e -> log.error("WOOZ "+_e.getMessage(), e), () -> log.info("WOOZ published: {}",_t)));
    }

    public static <K,V,T,R> ConsumerRecord<T,R> wrapConsumerRecord(ConsumerRecord<K, V> r, Function<K, T> keyMapper, Function<V, R> valueMapper) {
        return new ConsumerRecord<>(r.topic(),
                r.partition(),
                r.offset(),
                r.timestamp(),
                r.timestampType(),
                r.checksum(),
                r.serializedKeySize(),
                r.serializedValueSize(),
                keyMapper.apply(r.key()),
                valueMapper.apply(r.value()),
                r.headers(),
                r.leaderEpoch());
    }}
