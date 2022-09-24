package com.github.dfauth.trycatch;

import com.github.dfauth.functional.Try;
import org.junit.Test;

import java.util.Optional;

import static com.github.dfauth.trycatch.DispatchHandler.Consumer.toDispatcher;
import static com.github.dfauth.trycatch.DispatchHandler.Function.extract;
import static org.junit.Assert.*;

public class DispatchHandlerTestCase {

    @Test
    public void testFailureConsumer() {
        final Optional<Long>[] result = new Optional[]{Optional.<Long>empty()};
        Try<Long> t = Try.failure(new RuntimeException("Oops"));
        t.dispatch(toDispatcher(r -> result[0] = Optional.of(r)));
        assertTrue(result[0].isEmpty());
    }

    @Test
    public void testSuccessConsumer() {
        final Optional<Long>[] result = new Optional[]{Optional.empty()};
        Long ref = 1L;
        Try<Long> t = Try.success(ref);
        t.dispatch(toDispatcher(r -> result[0] = Optional.of(r)));
        assertTrue(result[0].isPresent());
        assertEquals(result[0].get(), ref);
    }

    @Test
    public void testFailureFunction() {
        Try<Long> t = Try.failure(new RuntimeException("Oops"));
        try {
            t.dispatch(DispatchHandler.Function.toDispatcher(Optional::of));
            fail("Oops");
        } catch (RuntimeException e) {
            // expected
        }
    }

    @Test
    public void testSuccessFunction() {
        Long ref = 1L;
        Try<Long> t = Try.success(ref);
        assertTrue(t.dispatch(DispatchHandler.Function.toDispatcher(Optional::of)).isPresent());
        assertEquals(t.dispatch(DispatchHandler.Function.toDispatcher(Optional::of)).orElseThrow(), ref);
    }

    @Test
    public void testIdentity() {
        Long ref = 1L;
        Try<Long> t = Try.success(ref);
        assertEquals(t.dispatch(extract()), ref);
    }

    @Test
    public void testIdentityFailure() {
        Try<Long> t = Try.failure(new RuntimeException("Oops"));
        try {
            t.dispatch(extract());
            fail("Oops");
        } catch (RuntimeException e) {
            // expected
        }
    }

}
