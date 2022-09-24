package com.github.dfauth.utils;

import com.github.dfauth.functional.Functions;
import com.github.dfauth.trycatch.AssertingLogger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;
import static com.github.dfauth.utils.Timer.withTimer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class TimerTest {

    @Test
    public void testIt0() throws Exception {
        String result = withTimer("A", () -> {
            sleep(100);
            return withTimer("B", () -> {
                sleep(100);
                return withTimer("C", () -> {
                    sleep(100);
                    return "D";
                });
            });
        });
        assertEquals("D", result);
        AssertingLogger.Call call = AssertingLogger.getQueue().poll();
        Optional<String> message = Optional.ofNullable(call).flatMap(c -> c.argument(0).map(Object::toString));
        message.map(m -> Functions.<String>peek(log::info)).orElseThrow();
    }

    @Test
    public void testIt() throws Exception {
        AtomicReference<List<Timer.Split>> x = new AtomicReference<>();
        Timer.register(x::set);
        String result = withTimer("A", () -> {
            sleep(100);
            return withTimer("B", () -> {
                sleep(100);
                return withTimer("C", () -> {
                    sleep(100);
                    return "D";
                });
            });
        });
        assertEquals("D", result);
        List<Timer.Split> splits = x.get();
        assertEquals(3, splits.size());
        Iterator<Timer.Split> it = splits.iterator();
        Timer.Split a = it.next();
        assertEquals("A", a.getKey());
        assertTrue(a.getState() instanceof Timer.Finished);
        assertTrue(((Timer.Finished)a.getState()).getElapsed() < 400);
        assertEquals(0,((Timer.Finished)a.getState()).getDepth());
        Timer.Split b = it.next();
        assertEquals("B", b.getKey());
        assertTrue(b.getState() instanceof Timer.Finished);
        assertTrue(((Timer.Finished)b.getState()).getElapsed() < 300);
        assertEquals(1,((Timer.Finished)b.getState()).getDepth());
        Timer.Split c = it.next();
        assertEquals("C", c.getKey());
        assertTrue(c.getState() instanceof Timer.Finished);
        assertTrue(((Timer.Finished)c.getState()).getElapsed() < 200);
        assertEquals(2,((Timer.Finished)c.getState()).getDepth());
    }

    public void sleep(long n) {
        tryCatch(() -> Thread.sleep(n));
    }
}
