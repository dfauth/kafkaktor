package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.ActorCreationRequestDispatchable;
import com.github.dfauth.kafka.KafkaSink;
import com.github.dfauth.kafka.RebalanceListener;
import com.github.dfauth.kafka.StreamBuilder;
import com.github.dfauth.kafkaktor.st8.KafkaConsumerEvent;
import com.github.dfauth.kafkaktor.st8.KafkaConsumerState;
import com.github.dfauth.st8.Event;
import com.github.dfauth.st8.State;
import com.github.dfauth.st8.StateMachine;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.github.dfauth.kafka.RebalanceListener.*;
import static com.github.dfauth.kafkaktor.st8.KafkaConsumerEvent.ASSIGNMENT;
import static com.github.dfauth.kafkaktor.st8.KafkaConsumerEvent.REVOCATION;
import static com.github.dfauth.kafkaktor.st8.KafkaConsumerStateMachine.buildStateMachine;

public class KafkaContext {

    private final String topic;
    private final StateMachine<KafkaConsumerState, KafkaContext, KafkaConsumerEvent, Collection<TopicPartition>, Collection<TopicPartition>> stateMachine;
    private final AktorSystem system;

    public KafkaContext(String topic, AktorSystem system) {
        this.system = system;
        this.topic = topic;
        this.stateMachine = buildStateMachine(this);
    }

    public String topic() {
        return topic;
    }

    public State<KafkaConsumerState, KafkaContext, KafkaConsumerEvent, Collection<TopicPartition>, Collection<TopicPartition>> currentState() {
        return stateMachine.currentState();
    }

    public <R extends SpecificRecord> CompletableFuture<AktorReference<R>> spawn(String key, Class<? extends Aktor<R>> aktorClass) {
        KafkaSink<String, SpecificRecord> sink = KafkaSink.<SpecificRecord>newStringKeyBuilder()
                .withProperties(system.config)
                .withValueSerializer(system.serializer())
                .withTopic(Supervisor.TOPIC)
                .build();
        return sink.publish(Supervisor.KEY, ActorCreationRequestDispatchable.newRequest(key, aktorClass)).thenApply(m ->
         new KafkaAktorReference(new KafkaAktorContext(key, this), new AktorAddress(key, m.topic(), m.partition())));
    }

    public AktorSystem aktorSystem() {
        return system;
    }

    public <R extends SpecificRecord> StreamBuilder<String, byte[],String,R> createStream(String key, Consumer<ConsumerRecord<String, R>> consumer) {
        Deserializer<R> deserializer = system.deserializer();
        return StreamBuilder.<byte[],R>stringKeyBuilder()
                .withProperties(aktorSystem().config, ConsumerConfig.GROUP_ID_CONFIG, key)
                .withTopic(topic)
                .withValueDeserializer((_t, _d) -> _d)
                .withValueMapper(b -> deserializer.deserialize(topic, b))
                .onPartitionAssignment(assignmentListener())
                .onPartitionRevocation(revocationListener())
                .withRecordConsumer(consumer)
                ;
    }

    public RebalanceListener<String, byte[]> assignmentListener() {
        return assignmentListener(noOp());
    }

    public RebalanceListener<String, byte[]> assignmentListener(RebalanceListener<String, byte[]> composeWith) {
        return composeWith.compose(offsetsFuture(tps -> stateMachine.onEvent(Event.<KafkaConsumerEvent, Collection<TopicPartition>>builder().withType(ASSIGNMENT).withPayload(tps.keySet()).build(), this)));
    }

    public RebalanceListener<String, byte[]> revocationListener() {
        return revocationListener(noOp());
    }

    public RebalanceListener<String, byte[]> revocationListener(RebalanceListener<String, byte[]> composeWith) {
        return composeWith.compose(offsetsFuture(tps -> stateMachine.onEvent(Event.<KafkaConsumerEvent, Collection<TopicPartition>>builder().withType(REVOCATION).withPayload(tps.keySet()).build(), this)));
    }

    public <T extends SpecificRecord> Serializer<T> serializer() {
        return system.serializer();
    }

    public <T extends SpecificRecord> Deserializer<T> deserializer() {
        return system.deserializer();
    }

}
