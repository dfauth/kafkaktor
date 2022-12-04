package com.github.dfauth.kafkaktor;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Map;
import java.util.function.Consumer;

import static com.github.dfauth.kafka.utils.PrimitiveHeader.fromHeaders;

public interface MessageContextAware<T> {

    static <R extends SpecificRecord> Consumer<ConsumerRecord<String, R>> toConsumerRecordConsumer(MessageContextAware<Consumer<R>> consumer, KafkaAktorContext ktx) {
        return r -> consumer.withMessageContext(new AktorMessageContext() {
            @Override
            public String key() {
                return r.key();
            }

            @Override
            public Map<String, Object> metadata() {
                return fromHeaders(r.headers());
            }

            @Override
            public AktorReference<R> sender() {
                return null;
            }
        }).accept(r.value());
    }

    T withMessageContext(AktorMessageContext m);

}
