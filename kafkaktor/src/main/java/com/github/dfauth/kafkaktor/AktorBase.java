package com.github.dfauth.kafkaktor;

import com.github.dfauth.kafka.Offsets;
import com.github.dfauth.kafka.StreamBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.github.dfauth.functional.Maps.mapEntryTransformer;
import static com.github.dfauth.kafka.RebalanceListener.seekToBeginning;
import static com.github.dfauth.kafkaktor.MessageContextAware.toConsumerRecordConsumer;

@Slf4j
public abstract class AktorBase<T extends SpecificRecord> implements Aktor<T> {

    protected final KafkaAktorContext ctx;
    private StreamBuilder.KafkaStream<String, byte[]> stream;

    protected AktorBase(KafkaAktorContext ctx) {
        this.ctx = ctx;
    }

    protected abstract String topic();

    public CompletableFuture<AktorAddress> start() {
        Consumer<ConsumerRecord<String, T>> c = toConsumerRecordConsumer(this.withAktorContext(this.ctx), ctx);
        stream = onStartup(ctx.kafkaContext().createStream(ctx.key(), c)).build();
        CompletableFuture<Map<TopicPartition, Offsets>> f = stream.start();
        return f.thenApply(tps -> tps.entrySet().stream().findFirst()
                        .map(mapEntryTransformer((tp,o) -> new AktorAddress(ctx.key(), tp.topic(), tp.partition())))
                        .orElseThrow(() -> new IllegalStateException("Oops. Should never happen")));
    }

    protected <R extends SpecificRecord> StreamBuilder<String, byte[], String, R> onStartup(StreamBuilder<String, byte[], String, R> builder) {
        return builder
                .onPartitionAssignment(ctx.kafkaContext().assignmentListener(seekToBeginning()));
    }

    @Override
    public void stop() {
        stream.stop();
    }

}
