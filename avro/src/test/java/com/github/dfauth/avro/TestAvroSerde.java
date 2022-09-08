package com.github.dfauth.avro;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;

public class TestAvroSerde extends KafkaAvroSerde {

    public TestAvroSerde() {
        super(new MockSchemaRegistryClient(), "dummy", true);
    }
}
