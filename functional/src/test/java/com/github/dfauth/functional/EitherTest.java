package com.github.dfauth.functional;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class EitherTest {

    @Test
    public void testIt() {
        {
            Either<Integer, String> e = Either.createLeft(1);
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
            e.acceptLeft(_l::set);
            assertEquals(1, (int)_l.get());
            assertTrue(e.mapLeft(String::valueOf).isPresent());
            assertEquals("1", e.mapLeft(String::valueOf).orElseThrow());
            assertFalse(e.mapRight(String::valueOf).isPresent());
        }
        {
            Either<Integer, String> e = Either.createRight("1");
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
            e.acceptRight(_r::set);
            assertEquals("1", _r.get());
            assertFalse(e.mapLeft(String::valueOf).isPresent());
            assertEquals("1", e.mapRight(String::valueOf).orElseThrow());
            assertTrue(e.mapRight(String::valueOf).isPresent());
            assertEquals("1", e.mapRight(String::valueOf).orElseThrow());
        }
    }
}
