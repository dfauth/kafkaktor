package com.github.dfauth.functional;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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
        {
            Either<Even, Odd> e = Thingy.create(1);
            assertEquals("1 is Odd", e.map(l -> l.get() + " is Even", r -> r.get() + " is Odd"));
        }
    }

    interface Thingy extends Supplier<Integer> {

        static boolean isEven(int i) {
            return i%2 == 0;
        }

        Integer get();

        static Either<Even, Odd> create(int n) {
            return isEven(n) ? Either.createLeft(even(n)) : Either.createRight(odd(n));
        }

        static Even even(int n) {
            return new Even(n);
        }

        static Odd odd(int n) {
            return new Odd(n);
        }
    }

    static class Even implements Thingy {

        private final int n;

        Even(int n) {
            if(!Thingy.isEven(n)) {
                throw new IllegalArgumentException(String.format("Oops, %d is not even",n));
            }
            this.n = n;
        }

        @Override
        public Integer get() {
            return this.n;
        }
    }

    static class Odd implements Thingy {

        private final int n;

        Odd(int n) {
            if(Thingy.isEven(n)) {
                throw new IllegalArgumentException(String.format("Oops, %d is not odd",n));
            }
            this.n = n;
        }

        @Override
        public Integer get() {
            return this.n;
        }
    }
}
