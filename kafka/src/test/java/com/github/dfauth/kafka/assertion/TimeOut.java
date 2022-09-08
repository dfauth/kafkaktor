package com.github.dfauth.kafka.assertion;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
public class TimeOut {

    private final int n;
    private final TimeUnit u;

    public TimeOut(int n, TimeUnit u) {
        this.n = n;
        this.u = u;
    }

    public static TimeOut of(int n, TimeUnit u) {
        return new TimeOut(n, u);
    }

    public <T> Executor<T> waitFor(CompletableFuture<T> f) {
        return () -> {
            try {
                return f.get(n, u);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        };
    }

    interface Executor<T> {

        default void andThen(Consumer<T> c) throws TimeoutException {
            c.accept(get());
        }

        T get() throws TimeoutException;
    }
}
