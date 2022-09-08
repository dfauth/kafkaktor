package com.github.dfauth.trycatch;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dfauth.trycatch.AssertingLogger.*;
import static com.github.dfauth.trycatch.TryCatch.Builder.tryCatch;
import static org.junit.Assert.*;

public class TryCatchBuilderTestCase {

    private static final Exception oops = new Exception("Oops");
    private static final Object ref = new Object();
    private static final Callable<Object> thrower = () -> { throw oops; };
    private static final Callable<Object> returner = () -> ref;
    private static final ExceptionalRunnable throwingRunner = () -> { throw oops; };
    private static AtomicBoolean wasRun = new AtomicBoolean(false);
    private static final Runnable runner = () -> wasRun.set(true);

    @Test
    public void testReThrowAndRun() {
        withCleanup(() -> {
            try {
                tryCatch(() -> thrower.call()).rethrowAndRun();
                fail("Oops");
            } catch (Exception e) {
                // expected
                assertExceptionLogged(oops);
            }
            resetLogEvents();
            try {
                tryCatch(() -> throwingRunner.run()).rethrowAndRun();
                fail("Oops");
            } catch (Exception e) {
                // expected
                assertExceptionLogged(oops);
            }
            resetLogEvents();
            assertEquals(ref, tryCatch(returner).rethrowAndRun());
            assertNothingLogged();
            resetLogEvents();
            TryCatch.Builder.tryCatch(runner).rethrowAndRun();
            assertTrue(wasRun.get());
            assertNothingLogged();
        });
    }

    @Test
    public void testReThrowAndBuild() {
        withCleanup(() -> {
            try {
                tryCatch(() -> thrower.call()).rethrow().run();
                fail("Oops");
            } catch (Exception e) {
                // expected
                assertExceptionLogged(oops);
            }
            resetLogEvents();
            assertEquals(ref, tryCatch(returner).rethrow().run());
            assertNothingLogged();
            tryCatch(runner).rethrow().run();
            assertTrue(wasRun.get());
            assertNothingLogged();
        });
    }

    @Test
    public void testIgnoreSilentlyAndRun() {
        withCleanup(() -> {
            tryCatch(() -> thrower.call()).ignoreSilentlyAndRun();
            assertNothingLogged();
        });
    }
}
