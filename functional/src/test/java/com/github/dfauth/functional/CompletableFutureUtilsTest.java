package com.github.dfauth.functional;

import com.github.dfauth.trycatch.AsyncUtil;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.github.dfauth.functional.CompletableFutureUtils.CompletionConsumer.onSuccess;
import static com.github.dfauth.functional.CompletableFutureUtils.CompletionConsumer.propagateTo;
import static com.github.dfauth.functional.CompletableFutureUtils.CompletionHandler;
import static org.junit.Assert.*;

public class CompletableFutureUtilsTest {

    private static final Object REF = new Object();
    private static final String REF1 = "1";
    private static final int REF2 = 1;

    @Test
    public void testConsumer() throws ExecutionException, InterruptedException {
        {
            CompletableFuture<Object> f = new CompletableFuture<>();
            CompletableFuture<Object> _f = new CompletableFuture<>();
            f.whenComplete(onSuccess(t -> _f.complete(t)).onFailure(_f::completeExceptionally));
            AsyncUtil.executeAsync(() -> f.complete(REF));
            assertEquals(_f.get(), REF);
        }
        {
            CompletableFuture<Object> f = new CompletableFuture<>();
            CompletableFuture<Object> _f = new CompletableFuture<>();
            f.whenComplete(onSuccess(t -> _f.complete(t)).onFailure(_f::completeExceptionally));
            RuntimeException oops = new RuntimeException("Oops");
            AsyncUtil.executeAsync(() -> f.completeExceptionally(oops));
            try {
                f.get();
                fail("Oops");
            } catch(ExecutionException e) {
                // expected
                assertEquals(oops, e.getCause());
            }
        }
    }

    @Test
    public void testFunction() throws ExecutionException, InterruptedException {
        {
            CompletableFuture<String> f = new CompletableFuture<>();
            CompletableFuture<Integer> result = f.handle(CompletionHandler.onSuccess(Integer::parseInt));
            AsyncUtil.executeAsync(() -> f.complete(REF1));
            assertEquals(REF2, (int)result.get());
        }
        {
            CompletableFuture<String> f = new CompletableFuture<>();
            CompletableFuture<Integer> result = f.handle(CompletionHandler.onSuccess(Integer::parseInt));
            AsyncUtil.executeAsync(() -> f.complete("Oops"));
            try {
                result.get();
                fail("Oops");
            } catch(ExecutionException e) {
                // expected
                assertEquals(NumberFormatException.class, e.getCause().getClass());
            }
        }
        {
            CompletableFuture<String> f = new CompletableFuture<>();
            CompletableFuture<Integer> result = f.handle(CompletionHandler.<String, Integer>onSuccess(Integer::parseInt).onFailure(ex -> 2));
            RuntimeException oops = new RuntimeException("Oops");
            AsyncUtil.executeAsync(() -> f.completeExceptionally(oops));
            assertEquals(2, (int)result.get());
        }
    }

    @Test
    public void testPropagation() throws ExecutionException, InterruptedException {
        {
            CompletableFuture<Object> f = new CompletableFuture<>();
            CompletableFuture<Object> _f = new CompletableFuture<>();
            f.whenComplete(propagateTo(_f));
            AsyncUtil.executeAsync(() -> f.complete(REF));
            assertEquals(_f.get(), REF);
        }
        {
            CompletableFuture<Object> f = new CompletableFuture<>();
            CompletableFuture<Object> _f = new CompletableFuture<>();
            f.whenComplete(propagateTo(_f, _ignored -> REF1));
            AsyncUtil.executeAsync(() -> f.complete(REF));
            assertEquals(REF1, _f.get());
        }
        {
            CompletableFuture<Object> f = new CompletableFuture<>();
            CompletableFuture<Object> _f = new CompletableFuture<>();
            f.whenComplete(propagateTo(_f));
            RuntimeException oops = new RuntimeException("Oops");
            AsyncUtil.executeAsync(() -> f.completeExceptionally(oops));
            try {
                _f.get();
                fail("Oops");
            } catch(ExecutionException e) {
                // expected
                assertEquals(oops, e.getCause());
            }
        }
    }
}
