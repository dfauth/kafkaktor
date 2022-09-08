package com.github.dfauth.avro;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class KafkaAvroSerde {

    private final Map<String, Object> config;
    private KafkaAvroDeserializer deserializer;
    private KafkaAvroSerializer serializer;

    public KafkaAvroSerde(SchemaRegistryClient schemaRegistryClient, String schemaRegistryUrl, boolean isAutoRegisterSchema) {
        this.config = Map.of(
                "specific.avro.reader", true,
                "auto.register.schema", isAutoRegisterSchema,
                "schema.registry.url", schemaRegistryUrl
        );
        this.serializer = new KafkaAvroSerializer(schemaRegistryClient, config);
        this.deserializer = new KafkaAvroDeserializer(schemaRegistryClient, config);
    }

    public <T extends SpecificRecord> Serde<T> serde() {
        return new Serde<>() {
            @Override
            public Serializer<T> serializer() {
                return (t,o) -> serializer.serialize(t,o);
            }

            @Override
            public Deserializer<T> deserializer() {
                return (t,o) -> (T) deserializer.deserialize(t,o);
            }
        };
    }
}
