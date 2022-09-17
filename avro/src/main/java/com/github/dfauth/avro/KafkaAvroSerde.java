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

    private final KafkaAvroDeserializer deserializer;
    private final KafkaAvroSerializer serializer;

    public KafkaAvroSerde(SchemaRegistryClient schemaRegistryClient, String schemaRegistryUrl, boolean isAutoRegisterSchema) {
        Map<String, Object> config = Map.of(
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
                return serializer::serialize;
            }

            @Override
            public Deserializer<T> deserializer() {
                return (t,o) -> (T) deserializer.deserialize(t,o);
            }
        };
    }
}
