package com.github.dfauth.kafkaktor;

import com.github.dfauth.kafka.KafkaSink;
import org.apache.avro.specific.SpecificRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.dfauth.kafka.utils.KafkaUtils.logErrors;

public class AktorAddress {

    private final String key;
    private final String topic;
    private final Optional<Integer> partition;

    public AktorAddress(String key, String topic) {
        this.key = key;
        this.topic = topic;
        this.partition = Optional.empty();
    }

    public AktorAddress(String key, String topic, int partition) {
        this.key = key;
        this.topic = topic;
        this.partition = Optional.of(partition);
    }

    public Optional<Integer> partition() {
        return partition;
    }

    public String topic() {
        return topic;
    }

    public String key() {
        return key;
    }

    public <T extends SpecificRecord> void publish(KafkaSink<String, T> s, T t, Map<String, Object> m) {
        partition.ifPresentOrElse(
                p -> logErrors(s.publish(topic, p, key, t, m)),
                () -> logErrors(s.publish(topic, key, t, m))
        );
    }

    Map<String, Object> metadata(AktorAddress address) {
        return metadata(Collections.emptyMap(), address);
    }

    Map<String, Object> metadata(Map<String, Object> h) {
        return metadata(h, this);
    }

    Map<String, Object> metadata(Map<String, Object> h, AktorAddress address) {
        Map<String, Object> tmp = new HashMap<>(h);
        tmp.put(AktorMessageContext.SENDER_KEY, address.key());
        return tmp;
    }
}
