package com.github.dfauth.functional;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.github.dfauth.functional.Lazy.lazy;
import static org.junit.Assert.*;

public class LazyTest {

    private final AtomicInteger triggerCount = new AtomicInteger(0);
    private final Supplier<Integer> testSupplier = () -> {
        triggerCount.incrementAndGet();
        return (Integer) 1;
    };

    @Test
    public void testIt() {
        TestIt x = TestIt.from(testSupplier, 2);
        assertTrue(x.isLessThan());
        assertEquals(1, triggerCount.get());
        assertFalse(x.isEqualsTo());
        assertEquals(1, triggerCount.get());
        assertFalse(x.isGreaterThan());
        assertEquals(1, triggerCount.get());
    }

    static class TestIt {

        private final Lazy<Boolean> isLessThan;
        private final Lazy<Boolean> isEqualsTo;
        private final Lazy<Boolean> isGreaterThan;

        public TestIt(Supplier<Integer> testSupplier, int i) {
            Lazy<Integer> l = lazy(testSupplier);
            this.isLessThan = l.map(_i -> _i < i);
            this.isEqualsTo = l.map(_i -> _i == i);
            this.isGreaterThan = l.map(_i -> _i > i);
        }

        public static TestIt from(Supplier<Integer> testSupplier, int i) {
            return new TestIt(testSupplier, i);
        }

        public boolean isLessThan() {
            return isLessThan.get();
        }

        public boolean isEqualsTo() {
            return isEqualsTo.get();
        }

        public boolean isGreaterThan() {
            return isGreaterThan.get();
        }
    }
}
