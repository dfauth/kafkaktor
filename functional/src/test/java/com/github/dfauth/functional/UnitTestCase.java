package com.github.dfauth.functional;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.dfauth.functional.Unit.Function0.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class UnitTestCase {

    @Test
    public void testUnit() {
        AtomicInteger ref = new AtomicInteger(-1);
        Consumer<Integer> c = ref::set;
        Function<Integer,Integer> f = i -> {
            ref.set(i);
            return i;
        };

        Unit.Function0<Integer> f2 = ref::set;

        {
            int i = 1;
            doit(i, c);
            assertEquals(i, ref.get());
            ref.set(-1);
        }

        {
            int i = 1;
            doit(i, of(f));
            assertEquals(i, ref.get());
            ref.set(-1);
        }

        {
            int i = 1;
            doit(i, f2);
            assertEquals(i, ref.get());
            ref.set(-1);
        }
    }

    private void doit(Integer i,Consumer<Integer> c) {
        c.accept(i);
    }

    @Test
    public void testMap() {
        Unit.Function0<Integer> f = i -> {};
        assertTrue(Optional.of(1).map(f).isEmpty());
    }
}
