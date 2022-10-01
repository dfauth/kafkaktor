package com.github.dfauth.kafka.assertion;

import com.github.dfauth.functional.Unit;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.github.dfauth.functional.CompletableFutureUtils.CompletionConsumer.onSuccess;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

public interface Assertions extends Callable<CompletableFuture<Void>> {

    static Builder builder() {
        return new Builder();
    }

    default void performAssertions() {
        try {
            call().join();
        } catch (CompletionException e) {
            try {
                throw e.getCause();
            } catch (Error|RuntimeException e1) {
                throw e1;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Slf4j
    class Builder {

        private List<CompletableFuture<Void>> outputs = new ArrayList<>();

        @SafeVarargs
        public final <T> OptionalQueue<CompletableFuture<T>> assertThat(Consumer<T>... c) {
            Queue<CompletableFuture<T>> q = new ArrayBlockingQueue<>(c.length);
            IntStream.rangeClosed(0,c.length-1).mapToObj(i -> assertThat(c[i])).forEach(q::offer);
            return () -> Optional.ofNullable(q.poll());
        }

        public <T> OptionalQueue<CompletableFuture<T>> assertThat(Consumer<T> c, int n) {
            Queue<CompletableFuture<T>> q = new ArrayBlockingQueue<>(n);
            IntStream.rangeClosed(0,n-1).mapToObj(ignored -> assertThat(c)).forEach(q::offer);
            return () -> Optional.ofNullable(q.poll());
        }

        public <T> CompletableFuture<T> assertThat(Consumer<T> c) {
            CompletableFuture<T> _f = new CompletableFuture<>();
            outputs.add(_f.thenApply(t -> Unit.<T,Void>toFunction(c).apply(t)));
            return _f;
        }

        public void build(CompletableFuture<Assertions> f) {

            CompletableFuture.allOf(
                    outputs.toArray(new CompletableFuture[outputs.size()])
            ).whenComplete(
                    onSuccess(ignored -> f.complete(complete(completedFuture(null))))
                            .onFailure(t -> f.complete(complete(failedFuture(t))))
            );
        }

        private Assertions complete(CompletableFuture<Void> x) {
            return () -> {
                // assertions are reduced to a boolean value -> only if all succeed can true is returned
                // block waiting for all futures to complete
                return outputs.stream().reduce(x, (f1,f2) -> CompletableFuture.allOf(f1,f2));
            };
        }
    }

    interface OptionalQueue<T> {
        Optional<T> poll();
    }
}
