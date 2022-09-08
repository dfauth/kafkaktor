package com.github.dfauth.functional;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.dfauth.functional.Unit.Function.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnitTestCase {

    private static final Logger logger = LoggerFactory.getLogger(UnitTestCase.class);

    @Test
    public void testUnit() {
        AtomicInteger ref = new AtomicInteger(-1);
        Consumer<Integer> c = i -> ref.set(i);
        Function<Integer,Integer> f = i -> {
            ref.set(i);
            return i;
        };

        Unit.Function<Integer> f2 = i -> ref.set(i);

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
        Unit.Function<Integer> f = i -> {};
        assertTrue(Optional.of(1).map(f).isPresent());
    }
}
