package com.github.dfauth.trycatch;

import com.github.dfauth.functional.Try;
import com.github.dfauth.functional.Unit;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dfauth.trycatch.AssertingLogger.*;
import static com.github.dfauth.trycatch.AsyncUtil.executeAsync;
import static com.github.dfauth.trycatch.TryCatch._Runnable.withExceptionLogging;
import static java.lang.Thread.sleep;
import static org.junit.Assert.*;

public class AsyncTestCase {

    @Before
    public void setUp() {
        resetLogEvents();
    }

    @Test
    public void testCallable() throws InterruptedException, ExecutionException, TimeoutException {

        {
            CompletableFuture<Try<Integer>> f = executeAsync(() -> 1).thenApply(i -> Try.tryWithCallable(() -> 2/i));
            Try<Integer> result = f.get(1, TimeUnit.SECONDS);
            assertTrue(result.isSuccess());
            assertEquals(2, result.toSuccess().result().intValue());
            assertNothingLogged();
        }

        {
            CompletableFuture<Integer> f = executeAsync(() -> 0).thenApply(i -> 2/i);
            assertThrows(ExecutionException.class, () -> f.get(1, TimeUnit.SECONDS));
            assertNothingLogged();
        }

        {
            CompletableFuture<Try<Integer>> f = executeAsync(() -> 0).thenApply(i -> Try.tryWithCallable(() -> 2/i));
            Try<Integer> result = f.get(1, TimeUnit.SECONDS);
            assertTrue(result.isFailure());
            assertThrows(ArithmeticException.class, () -> result.toFailure().throwException());
            assertExceptionLogged(new ArithmeticException("/ by zero"));
        }
    }

    @Test
    public void testRunnable() throws InterruptedException, ExecutionException, TimeoutException {

        {
            CompletableFuture<Unit> f = executeAsync(() -> {});
            Unit result = f.get(1, TimeUnit.SECONDS);
            assertNothingLogged();
        }

        {
            AtomicReference<Optional<Integer>> blah = new AtomicReference<>();
            CompletableFuture<Void> f = executeAsync(() -> {
                blah.set(Optional.of(1));
            }).thenAccept(withExceptionLogging(_unit -> blah.get().ifPresent(b -> blah.set(Optional.of(2/b)))));
            sleep(100);
            assertEquals(2, blah.get().get().intValue());
            assertNothingLogged();
        }

        {
            AtomicReference<Optional<Integer>> blah = new AtomicReference<>();
            CompletableFuture<Void> f = executeAsync(() -> {
                blah.set(Optional.of(0));
            }).thenAccept(withExceptionLogging(_unit -> blah.get().ifPresent(b -> blah.set(Optional.of(2/b)))));
            assertThrows(ExecutionException.class, () -> f.get(1, TimeUnit.SECONDS));
            assertExceptionLogged(new ArithmeticException("/ by zero"));
        }

    }

}
