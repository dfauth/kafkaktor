package com.github.dfauth.kafkaktor.st8;

import com.github.dfauth.kafkaktor.KafkaContext;
import com.github.dfauth.st8.StateMachine;
import com.github.dfauth.st8.TransitionListener;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Optional;

import static com.github.dfauth.kafkaktor.st8.KafkaConsumerEvent.ASSIGNMENT;
import static com.github.dfauth.kafkaktor.st8.KafkaConsumerEvent.REVOCATION;
import static com.github.dfauth.kafkaktor.st8.KafkaConsumerState.*;
import static com.github.dfauth.st8.TransitionListener.eventPayload;

public class KafkaConsumerStateMachine {

    public static StateMachine<KafkaConsumerState, KafkaContext, KafkaConsumerEvent, Collection<TopicPartition>, Collection<TopicPartition>> buildStateMachine(KafkaContext ctx) {

        StateMachine.Builder<KafkaConsumerState, KafkaContext, KafkaConsumerEvent, Collection<TopicPartition>, Collection<TopicPartition>> builder = StateMachine.create("partitionAssignment", ctx);

        builder.initial(INITIAL)
                .onEvent(ASSIGNMENT)
                .onTransition(onAssignment())
                .goTo(ASSIGNED)
                .state(ASSIGNED)
                .onEvent(REVOCATION)
                .goTo(REVOKED)
                .onTransition(onRevocation())
                .state(REVOKED);

        return builder.build();
    }

    private static TransitionListener<KafkaConsumerState, KafkaContext, KafkaConsumerEvent, Collection<TopicPartition>, Collection<TopicPartition>> onAssignment() {
        return eventPayload(e -> e.payload());
    }

    private static TransitionListener<KafkaConsumerState, KafkaContext, KafkaConsumerEvent, Collection<TopicPartition>, Collection<TopicPartition>> onRevocation() {
        return eventPayload(e -> Optional.empty());
    }


}
