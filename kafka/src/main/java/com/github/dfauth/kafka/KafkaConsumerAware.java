package com.github.dfauth.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;

public interface KafkaConsumerAware<K,V,T> {
    T withKafkaConsumer(KafkaConsumer<K,V> consumer);
}
