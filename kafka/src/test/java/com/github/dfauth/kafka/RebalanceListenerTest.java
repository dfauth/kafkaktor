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
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dfauth.kafka.RebalanceListener.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class RebalanceListenerTest {

    private static final String T = "TOPIC";
    private static final int P = 0;

    TopicPartition tp = new TopicPartition(T, P);
    long o = 1L;

    @Test
    public void testSeekToBeginning() {

        RebalanceListener<String,String> rebalanceListener = seekToBeginning();
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        when(c.position(any())).thenReturn(0L);
        rebalanceListener.withKafkaConsumer(c).accept(List.of(tp));
        verify(c, times(0)).position(any());
        verify(c, times(1)).seekToBeginning(any());
        verify(c, times(0)).seekToEnd(any());
        verify(c, times(0)).seek(any(), any());
    }


    @Test
    public void testSeekToEnd() {

        RebalanceListener<String,String> rebalanceListener = seekToEnd();
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        when(c.position(any())).thenReturn(0L);
        rebalanceListener.withKafkaConsumer(c).accept(List.of(tp));
        verify(c, times(0)).position(any());
        verify(c, times(0)).seekToBeginning(any());
        verify(c, times(1)).seekToEnd(any());
        verify(c, times(0)).seek(any(), any());
    }

    @Test
    public void testNoOp() {

        RebalanceListener<String, String> rebalanceListener = noOp();
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        when(c.position(any())).thenReturn(0L);
        Collection<TopicPartition> tps = Collections.singletonList(tp);
        rebalanceListener.withKafkaConsumer(c).accept(List.of(tp));
        verify(c, times(0)).position(any());
        verify(c, times(0)).seekToBeginning(tps);
        verify(c, times(0)).seekToEnd(tps);
        verify(c, times(0)).seek(any(), any());
    }

    @Test
    public void testSeekToOffsets() {

        RebalanceListener<String,String> rebalanceListener = seekToOffset(Map.of(tp, 0L));
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        when(c.position(any())).thenReturn(0L);
        Collection<TopicPartition> tps = Collections.singletonList(tp);
        rebalanceListener.withKafkaConsumer(c).accept(List.of(tp));
        verify(c, times(0)).position(any());
        verify(c, times(0)).seekToBeginning(tps);
        verify(c, times(0)).seekToEnd(tps);
        verify(c, times(1)).seek(tp, 0L);
    }

    @Test
    public void testSeekToTimestamps() {

        ZonedDateTime now = ZonedDateTime.now();

        RebalanceListener<String,String> rebalanceListener = seekToTimestamp(now.toInstant());
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        when(c.position(any())).thenReturn(0L);
        List<TopicPartition> tps = Collections.singletonList(tp);
        Map<TopicPartition, Long> times = Collections.singletonMap(tp, now.toInstant().toEpochMilli());
        when(c.offsetsForTimes(times)).thenReturn(Collections.singletonMap(tp, new OffsetAndTimestamp(o, now.toInstant().toEpochMilli())));
        rebalanceListener.withKafkaConsumer(c).accept(List.of(tp));
        verify(c, times(0)).position(any());
        verify(c, times(0)).seekToBeginning(tps);
        verify(c, times(0)).seekToEnd(tps);
        verify(c, times(1)).seek(tp, o);
    }

    @Test
    public void testAndThen() {

        AtomicBoolean called = new AtomicBoolean(false);
        RebalanceListener<String,String> rebalanceListener = RebalanceListener.<String,String>seekToBeginning().andThen(c -> tps -> called.set(true));
        KafkaConsumer<String, String> c = mock(KafkaConsumer.class);
        when(c.position(any())).thenReturn(0L);
        rebalanceListener.withKafkaConsumer(c).accept(List.of(tp));
        verify(c, times(0)).position(any());
        verify(c, times(1)).seekToBeginning(any());
        verify(c, times(0)).seekToEnd(any());
        verify(c, times(0)).seek(any(), any());
        assertTrue(called.get());
    }
}
