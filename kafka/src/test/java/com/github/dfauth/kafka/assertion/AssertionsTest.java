package com.github.dfauth.kafka.assertion;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static com.github.dfauth.functional.CompletableFutureUtils.CompletionConsumer.propagateTo;
import static com.github.dfauth.functional.CompletableFutureUtils.CompletionHandler.onSuccess;
import static com.github.dfauth.trycatch.ExceptionalRunnable.asRunnable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.*;

@Slf4j
public class AssertionsTest {

    private static final TimeOut TIMEOUT = TimeOut.of(500, MILLISECONDS);
    private Object REF1 = new Object();

    @Test
    public void testPass() throws TimeoutException {
        Assertions.Builder builder = Assertions.builder();
        CompletableFuture<Object> f1 = builder.assertThat(a1 -> assertEquals(REF1, a1));
        CompletableFuture<Assertions> f = new CompletableFuture<>();
        builder.build(f);
        Executors.newSingleThreadExecutor().execute(asRunnable(() -> {
            Thread.sleep(200);
            f1.complete(REF1);
        }));
        TIMEOUT.waitFor(f).andThen(Assertions::performAssertions);
    }

    @Test
    public void testAssertionFail() {
        try {
            Assertions.Builder builder = Assertions.builder();
            CompletableFuture<Object> f1 = builder.assertThat(Assert::assertNull);
            CompletableFuture<Assertions> f = new CompletableFuture<>();
            builder.build(f);
            Executors.newSingleThreadExecutor().execute(asRunnable(() -> {
                Thread.sleep(200);
                f1.complete(REF1);
            }));
            TIMEOUT.waitFor(f).andThen(Assertions::performAssertions);
        } catch (AssertionError e) {
            // expected
            return;
        } catch (TimeoutException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        fail("Oops. Expected AssertionError");
    }

    @Test
    public void testFail() {
        try {
            Assertions.Builder builder = Assertions.builder();
            CompletableFuture<Object> f1 = builder.assertThat(a1 -> assertEquals(REF1, a1));
            CompletableFuture<Assertions> f = new CompletableFuture<>();
            builder.build(f);
            Executors.newSingleThreadExecutor().execute(asRunnable(() -> {
                Thread.sleep(200);
                f1.completeExceptionally(new IllegalArgumentException("Oops"));
            }));
            TIMEOUT.waitFor(f).andThen(Assertions::performAssertions);
            fail("Oops. Expected AssertionError");
        } catch (IllegalArgumentException e) {
            // expected
        } catch (TimeoutException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testTimeout() {
        try {
            Assertions.Builder builder = Assertions.builder();
            CompletableFuture<Object> f1 = builder.assertThat(a1 -> assertEquals(REF1, a1));
            CompletableFuture<Assertions> f = new CompletableFuture<>();
            builder.build(f);
            TIMEOUT.waitFor(f).andThen(Assertions::performAssertions);
            fail("Oops. Expected TimeoutException");
        } catch (TimeoutException e) {
            // expected
        }
    }

    @Test
    public void testIt() throws ExecutionException, InterruptedException {
        // CompletableFuture will correctly propagate failures in any transforming function passed to it in a call to
        // thenApply...
        {
            CompletableFuture<String> f = new CompletableFuture<>();
            CompletableFuture<Integer> f1 = f.thenApply(Integer::parseInt);
            f.complete("1");
            assertTrue(f1.isDone());
            assertEquals(1, (int)f1.get());
        }
        {
            CompletableFuture<String> f = new CompletableFuture<>();
            CompletableFuture<Integer> f1 = f.thenApply(Integer::parseInt);
            f.complete("Oops");
            assertTrue(f1.isDone());
            try {
                assertTrue(f1.isDone());
                f1.get();
            } catch (ExecutionException e) {
                assertEquals(NumberFormatException.class, e.getCause().getClass());
            }
        }

        // propagateTo should do the same
        {
            CompletableFuture<String> f = new CompletableFuture<>();
            CompletableFuture<Integer> f1 = new CompletableFuture<>();
            f.whenComplete(propagateTo(f1,Integer::parseInt));
            f.complete("1");
            assertTrue(f1.isDone());
            assertEquals(1, (int)f1.get());
        }
        {
            CompletableFuture<String> f = new CompletableFuture<>();
            CompletableFuture<Integer> f1 = new CompletableFuture<>();
            f.whenComplete(propagateTo(f1,Integer::parseInt));
            f.complete("Oops");
            assertTrue(f1.isDone());
            try {
                assertTrue(f1.isDone());
                f1.get();
            } catch (ExecutionException e) {
                assertEquals(NumberFormatException.class, e.getCause().getClass());
            }
        }

        // similarly
        {
            CompletableFuture<String> f = new CompletableFuture<>();
            CompletableFuture<Integer> f1 = f.handle(onSuccess(Integer::parseInt));
            f.complete("1");
            assertTrue(f1.isDone());
            assertEquals(1, (int)f1.get());
        }
        {
            CompletableFuture<String> f = new CompletableFuture<>();
            CompletableFuture<Integer> f1 = f.handle(onSuccess(Integer::parseInt));
            f.complete("Oops");
            assertTrue(f1.isDone());
            try {
                assertTrue(f1.isDone());
                f1.get();
            } catch (ExecutionException e) {
                assertEquals(NumberFormatException.class, e.getCause().getClass());
            }
        }

    }

}
