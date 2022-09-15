package com.github.dfauth.functional;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class EitherTest {

    @Test
    public void testIt() {
        {
            Either<Integer, String> e = Either.left(1);
            assertTrue(e.isLeft());
            assertFalse(e.isRight());
            assertEquals(1, (int)e.left());
            try {
                e.right();
                fail("expected an IllegalStateException");
            } catch (IllegalStateException ex) {
                // expected
            }
            AtomicReference<Integer> _l = new AtomicReference<>();
            e.onLeft(_l::set);
            assertEquals(1, (int)_l.get());
        }
        {
            Either<Integer, String> e = Either.right("1");
            assertFalse(e.isLeft());
            assertTrue(e.isRight());
            assertEquals("1", e.right());
            try {
                e.left();
                fail("expected an IllegalStateException");
            } catch (IllegalStateException ex) {
                // expected
            }
            AtomicReference<String> _r = new AtomicReference<>();
            e.onRight(_r::set);
            assertEquals("1", _r.get());
        }
    }
}
