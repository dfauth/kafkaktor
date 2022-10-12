package com.github.dfauth.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dfauth.kafka.RebalanceListener.*;
import static com.github.dfauth.kafka.TopicPartitionOffset.replayMonitor;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class RebalanceListenerTest {

    private static final String T = "TOPIC";
    private static final int P = 0;

    TopicPartition tp = new TopicPartition(T, P);
    long o = 1L;

    @Test
    public void testSeekToBeginning() {

        RebalanceListener<String, String> rebalanceListener = seekToBeginning();
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        Collection<TopicPartition> tps = Collections.singletonList(tp);
        rebalanceListener.withKafkaConsumer(c).accept(tps);
        verify(c, times(1)).seekToBeginning(tps);
        verify(c, times(0)).seekToEnd(tps);
        verify(c, times(0)).seek(any(), any());
    }


    @Test
    public void testSeekToEnd() {

        RebalanceListener<String, String> rebalanceListener = seekToEnd();
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        Collection<TopicPartition> tps = Collections.singletonList(tp);
        rebalanceListener.withKafkaConsumer(c).accept(tps);
        verify(c, times(0)).seekToBeginning(tps);
        verify(c, times(1)).seekToEnd(tps);
        verify(c, times(0)).seek(any(), any());
    }

    @Test
    public void testNoOp() {

        RebalanceListener<String, String> rebalanceListener = noOp();
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        Collection<TopicPartition> tps = Collections.singletonList(tp);
        rebalanceListener.withKafkaConsumer(c).accept(tps);
        verify(c, times(0)).seekToBeginning(tps);
        verify(c, times(0)).seekToEnd(tps);
        verify(c, times(0)).seek(any(), any());
    }

    @Test
    public void testSeekToOffsets() {

        RebalanceListener<String, String> rebalanceListener = seekToOffsets(ignored -> o);
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        Collection<TopicPartition> tps = Collections.singletonList(tp);
        rebalanceListener.withKafkaConsumer(c).accept(tps);
        verify(c, times(0)).seekToBeginning(tps);
        verify(c, times(0)).seekToEnd(tps);
        verify(c, times(1)).seek(tp, o);
    }

    @Test
    public void testSeekToTimestamps() {

        ZonedDateTime now = ZonedDateTime.now();

        RebalanceListener<String, String> rebalanceListener = seekToTimestamp(now);
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        List<TopicPartition> tps = Collections.singletonList(tp);
        Map<TopicPartition, Long> times = Collections.singletonMap(tp, now.toInstant().toEpochMilli());
        when(c.offsetsForTimes(times)).thenReturn(Collections.singletonMap(tp, new OffsetAndTimestamp(o, now.toInstant().toEpochMilli())));
        rebalanceListener.withKafkaConsumer(c).accept(Collections.singletonList(tp));
        verify(c, times(0)).seekToBeginning(tps);
        verify(c, times(0)).seekToEnd(tps);
        verify(c, times(1)).seek(tp, o);
    }

    @Test
    public void testAndThen() {

        AtomicBoolean called = new AtomicBoolean(false);
        RebalanceListener<String, String> rebalanceListener = RebalanceListener.<String,String>seekToBeginning().andThen(cp -> tps -> called.set(true));
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        Collection<TopicPartition> tps = Collections.singletonList(tp);
        rebalanceListener.withKafkaConsumer(c).accept(tps);
        verify(c, times(1)).seekToBeginning(tps);
        verify(c, times(0)).seekToEnd(tps);
        verify(c, times(0)).seek(any(), any());
        assertTrue(called.get());
    }

    @Test
    public void testIt() throws ExecutionException, InterruptedException {

        TopicPartition tp = new TopicPartition(T, P);
        long o = 1L;

        CompletableFuture<TopicPartitionAware<ReplayMonitor>> f = new CompletableFuture<>();
        RebalanceListener<String, String> rebalanceListener = RebalanceListener.<String,String>seekToBeginning().compose(currentOffsets(tpm -> f.complete(replayMonitor(tpm))));
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        when(c.position(tp)).thenReturn(o);
        rebalanceListener.withKafkaConsumer(c).accept(Collections.singleton(tp));
        verify(c, times(1)).seekToBeginning(Collections.singleton(tp));
        assertTrue(f.get().withTopicPartition(tp.topic(), tp.partition()).isReplay(0));
        assertFalse(f.get().withTopicPartition(tp.topic(), tp.partition()).isReplay(o));
    }
}
