package com.github.dfauth.kafka;

import com.github.dfauth.functional.Try;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.github.dfauth.kafka.TopicPartitionOffset.*;
import static com.github.dfauth.functional.CompletableFutureUtils.asHandler;
import static com.github.dfauth.trycatch.DispatchHandler.Function.extract;
import static org.junit.Assert.*;

public class TopicPartitionOffsetTest {

    private static final String T = "TOPIC";
    private static final int P = 0;
    private static final TopicPartition TP = new TopicPartition(T,P);

    @Test
    public void testEquals() {
        long o = 1L;
        Map<TopicPartition, Long> ref = Map.of(TP,o);
        assertFalse(isRecovering.apply(ref).apply(TP,o).dispatch(extract()));
    }

    @Test
    public void testBefore() {
        long o = 1L;
        Map<TopicPartition, Long> ref = Map.of(TP,o);
        assertTrue(isRecovering.apply(ref).apply(TP,0L).dispatch(extract()));
    }

    @Test
    public void testAfter() {
        long o = 1L;
        Map<TopicPartition, Long> ref = Map.of(TP,o);
        assertFalse(isRecovering.apply(ref).apply(TP,2L).dispatch(extract()));
    }

    @Test
    public void testFailure() {
        long o = 1L;
        Map<TopicPartition, Long> ref = Map.of(TP,o);
        try {
            isRecovering.apply(ref).apply(new TopicPartition(T, 1),2L).dispatch(extract());
            fail("OOps");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testIsRecovering3After() {
        long o = 1L;
        Map<TopicPartition, Long> ref = Map.of(TP,o);
        assertFalse(replayMonitor(ref).apply(T,P).test(2L));
    }

    @Test
    public void testIsRecovering3Failure() {
        long o = 1L;
        Map<TopicPartition, Long> ref = Map.of(TP,o);
        try {
            replayMonitor(ref).apply(T,1).test(2L);
            fail("OOps");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testFutureRecovery() throws ExecutionException, InterruptedException {
        long o = 1L;
        Map<TopicPartition, Long> ref = Map.of(TP,o);
        CompletableFuture<BiFunction<String, Integer, Function<Long, Try<Boolean>>>> isRecoveringFuture = new CompletableFuture<>();
        CompletableFuture<Boolean> simplified = isRecoveringFuture.handle(asHandler(f -> f.apply(T, P).apply(0L).dispatch(extract())));
        ForkJoinPool.commonPool().execute(() -> {
            isRecoveringFuture.complete(isReplay2(ref));
        });
        assertTrue(simplified.get());
    }

    @Test
    public void testFutureRecovered() throws ExecutionException, InterruptedException {
        long o = 1L;
        Map<TopicPartition, Long> ref = Map.of(TP,o);
        CompletableFuture<BiFunction<String, Integer, Function<Long, Try<Boolean>>>> isRecoveringFuture = new CompletableFuture<>();
        CompletableFuture<Boolean> simplified = isRecoveringFuture.handle(asHandler(f -> f.apply(T, P).apply(2L).dispatch(extract())));
        ForkJoinPool.commonPool().execute(() -> {
            isRecoveringFuture.complete(isReplay2(ref));
        });
        assertFalse(simplified.get());
    }

    @Test
    public void testFutureFailure() {
        long o = 1L;
        Map<TopicPartition, Long> ref = Map.of(TP,o);
        CompletableFuture<BiFunction<String, Integer, Function<Long, Try<Boolean>>>> isRecoveringFuture = new CompletableFuture<>();
        CompletableFuture<Boolean> simplified = isRecoveringFuture.handle(asHandler(f -> f.apply(T, 1).apply(2L).dispatch(extract())));
        ForkJoinPool.commonPool().execute(() -> {
            isRecoveringFuture.complete(isReplay2(ref));
        });
        try {
            simplified.get();
            fail("Oops");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            // expected
        }
    }

}
