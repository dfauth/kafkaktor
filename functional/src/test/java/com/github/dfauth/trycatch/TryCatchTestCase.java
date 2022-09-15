package com.github.dfauth.trycatch;

import com.github.dfauth.functional.Try;
import com.github.dfauth.functional.Unit;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.*;

import static com.github.dfauth.functional.Functions.peek;
import static com.github.dfauth.functional.Try.tryWith;
import static com.github.dfauth.trycatch.AssertingLogger.*;
import static com.github.dfauth.trycatch.CallableFunction.toFunction;
import static com.github.dfauth.trycatch.ExceptionalConsumer.toConsumer;
import static com.github.dfauth.trycatch.TryCatch._Callable;
import static com.github.dfauth.trycatch.TryCatch._Runnable;
import static com.github.dfauth.trycatch.TryCatch._Runnable.withExceptionLogging;
import static org.junit.Assert.*;

public class TryCatchTestCase {

    private static final Logger logger = LoggerFactory.getLogger(TryCatchTestCase.class);
    private static final RuntimeException runtimeOops = new RuntimeException("Oops");
    private static final Exception oops = new Exception("Oops");

    private <T> T throwRuntimeOops() {
        throw runtimeOops;
    }

    private <T> T throwOops() throws Exception {
        throw oops;
    }

    @Before
    public void setUp() {
        resetLogEvents();
    }

    @Test
    public void testTryCatch() {

        // Runnable
        _Runnable.tryCatch(() -> {
        });
        assertNothingLogged();

        // Callable
        assertEquals(1, _Callable.tryCatch(() -> 1).intValue());
        assertNothingLogged();

        // void return throws exception
        _Runnable.tryCatch(() -> Thread.sleep(100));
        assertNothingLogged();

        // Runnable
        try {
            _Runnable.tryCatch(this::throwRuntimeOops);
            fail("Oops, expected exception");
        } catch (RuntimeException e) {
            // expected;
            assertExceptionLogged(runtimeOops);
        }

        // Callable
        try {
            _Callable.tryCatch(this::throwOops);
            fail("Oops, expected exception");
        } catch (RuntimeException e) {
            // expected;
            assertExceptionLogged(oops);
        }

        // void return throws exception
        try {
            _Runnable.tryCatch(this::throwOops);
            fail("Oops, expected exception");
        } catch (RuntimeException e) {
            // expected;
            assertExceptionLogged(oops);
        }
    }

    @Test
    public void testTryCatchIgnore() {

        _Runnable.tryCatchIgnore(this::throwOops);
        assertExceptionLogged(oops);

        _Runnable.tryCatchIgnore(() -> {
        });
        assertNothingLogged();

        assertEquals(1, _Callable.<Integer>tryCatchIgnore(this::throwOops, 1).intValue());
        assertExceptionLogged(oops);
    }

    @Test
    public void testWithExceptionLogging() throws InterruptedException, ExecutionException, TimeoutException {

        // Runnable
        Executors.newSingleThreadExecutor().submit(withExceptionLogging(() -> {})).get(1, TimeUnit.SECONDS);

        // Callable
        String result = "result";
        assertEquals(result, Executors.newSingleThreadExecutor().submit(_Callable.withExceptionLogging(() -> result)).get(1, TimeUnit.SECONDS));
        assertNothingLogged();

        // ExceptionalRunnable
        Future<?> f = Executors.newSingleThreadExecutor().submit(withExceptionLogging(() -> {
            throw oops;
        }));
        try {
            f.get(1, TimeUnit.SECONDS);
            fail("Oops. expected ExecutionException");
        } catch (InterruptedException | TimeoutException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            // expected
        }
        assertExceptionLogged(oops);

        // CallableFunction
        CallableFunction<CompletableFuture<String>, String> g = (CompletableFuture<String> _f) -> _f.get(1, TimeUnit.SECONDS);
        {
            resetLogEvents();
            assertEquals(result, Optional.of(CompletableFuture.completedFuture(result)).map(_Callable.withExceptionLogging(g)).get());
            assertNothingLogged();
        }
        {
            assertTrue(tryWith(() -> Optional.of(CompletableFuture.<String>failedFuture(oops)).map(_Callable.withExceptionLogging(g)).get()).isFailure());
            assertExceptionLogged(new ExecutionException(oops));
            assertExceptionLogged(new RuntimeException(new ExecutionException(oops))); // TODO why?
            assertNothingLogged();
        }

        // ExceptionalConsumer
        {
            assertTrue(tryWith(() -> Optional.of(CompletableFuture.completedFuture(result)).map(toFunction(CompletableFuture::get)).orElseThrow()).isSuccess());
            assertNothingLogged();
        }
        {
            assertTrue(tryWith(() -> Optional.of(CompletableFuture.completedFuture(result)).ifPresent(toConsumer(CompletableFuture::get))).isSuccess());
            assertNothingLogged();
        }
        {
            assertTrue(tryWith(() -> Optional.of(CompletableFuture.failedFuture(oops)).ifPresent(toConsumer(CompletableFuture::get))).isFailure());
            assertExceptionLogged(new ExecutionException(oops));
            assertExceptionLogged(new RuntimeException(new ExecutionException(oops))); // TODO why?
            assertNothingLogged();
        }

    }

    @Test
    public void testTheIgnorant() {

        // Runnable

        // Callable

        // ExceptionalRunnable

        // CallableFunction
        CallableFunction<CompletableFuture<String>, String> g = (CompletableFuture<String> _f) -> _f.get(1, TimeUnit.SECONDS);
        {
            assertEquals("oops", Optional.of(CompletableFuture.<String>failedFuture(oops)).map(_Callable.withExceptionLogging(g, "oops")).get());
            assertExceptionLogged(new ExecutionException(oops));
        }

    }

    @Test
    public void testTryWith() {
        {
            Try<Integer> t = Try.tryWithCallable(() -> 1);
            assertNotNull(t);
            assertTrue(t.isSuccess());
            Try<Integer> result = t.map(v -> 2*v);
            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(2, result.toOptional().get().intValue());
            assertNothingLogged();
        }

        {
            Try<Unit> t = tryWith(() -> {
                throw runtimeOops;
            });
            assertNotNull(t);
            assertTrue(t.isFailure());
            Try<Integer> result = t.map(v -> 1);
            assertNotNull(result);
            assertTrue(result.isFailure());
            assertExceptionLogged(runtimeOops);
            assertNothingLogged();
        }

        {
            Try<Integer> t = Try.tryWithCallable(() -> 0);
            assertNotNull(t);
            assertTrue(t.isSuccess());
            Try<Integer> result = t.map(v -> 1/v);
            assertNotNull(result);
            assertTrue(result.isFailure());
            assertTrue(result.toFailure().exception() instanceof ArithmeticException);
            assertEquals(result.toFailure().exception().getMessage(), "/ by zero");
            assertExceptionLogged(new ArithmeticException("/ by zero"));
        }
    }

    @Test
    public void testFlatMap() {
        {
            Try<Integer> t = Try.tryWithCallable(() -> 1);
            assertNotNull(t);
            assertTrue(t.isSuccess());
            Try<Integer> result = t.flatMap(v -> Try.tryWithCallable(() -> 2/v));
            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(2, result.toSuccess().result().intValue());
            assertNothingLogged();
        }

        {
            Try<Integer> t = Try.tryWithCallable(this::throwRuntimeOops);
            assertNotNull(t);
            assertTrue(t.isFailure());
            Try<Integer> result = t.flatMap(v -> Try.tryWithCallable(() -> 2/v));
            assertNotNull(result);
            assertTrue(result.isFailure());
            assertEquals(runtimeOops, result.toFailure().exception());
            assertExceptionLogged(runtimeOops);
        }

        {
            Try<Integer> t = Try.tryWithCallable(() -> 0);
            assertNotNull(t);
            assertTrue(t.isSuccess());
            Try<Integer> result = t.flatMap(v -> Try.tryWithCallable(() -> 2/v));
            assertNotNull(result);
            assertTrue(result.isFailure());
            assertThrows(ArithmeticException.class, () -> result.toFailure().throwException());
            assertExceptionLogged(new ArithmeticException("/ by zero"));
        }
    }

    @Test
    public void testRecover() {
        {
            Try<Integer> t = Try.success(1);
            t.map(peek(r -> logger.info("map: "+r)))
                    .recover(_t -> {
                        logger.error("recover: "+_t.getMessage(), t);
                        return null;
                    });
            assertInfoLogged(msg -> msg.startsWith("map: "));
        }
        {
            Try<Integer> t = Try.tryWithCallable(this::throwRuntimeOops);
            assertEquals(1, (int)t.map(peek(r -> logger.info("map: "+r)))
                    .recover(_t -> {
                        logger.info("recover: "+_t.getMessage());
                        return 1;
                    }).toOptional().get());
            assertExceptionLogged(runtimeOops);
            assertInfoLogged(msg -> msg.startsWith("recover: "));
        }
    }


}
