package com.github.dfauth.functional;

import com.github.dfauth.trycatch.TryCatch;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.github.dfauth.trycatch.TryCatch.CallableBuilder.loggingOperator;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;

@Slf4j
public class CompletableFutureUtils {

    public static <R> Function<Throwable, R> loggingFunction() {
        return loggingFunction(null);
    }

    public static <R> Function<Throwable, R> loggingFunction(R defaultValue) {
        return t -> {
            loggingOperator.apply(t);
            return defaultValue;
        };
    }

    public static <T, R> BiFunction<T, Throwable, R> asHandler(Function<T, R> mapper) {
        return asHandlers(mapper, loggingFunction());
    }

    public static <T, R> BiFunction<T, Throwable, R> asHandlers(Function<T, R> mapper, Function<Throwable, R> handler) {
        return (t, e) -> Optional.ofNullable(e).map(handler).orElseGet(() -> mapper.apply(t));
    }

    public static <T> Iterable<T> completed(Iterable<CompletableFuture<T>> futures) {
        return StreamSupport.stream(futures.spliterator(), true).filter(CompletableFuture::isDone).map(f -> TryCatch._Callable.tryCatch(f::get)).collect(Collectors.toList());
    }

    public static class CompletionConsumer<T> implements BiConsumer<T, Throwable> {

        private final Consumer<T> successConsumer;
        private final Consumer<Throwable> failureConsumer;

        public CompletionConsumer(Consumer<T> consumer) {
            this(consumer, t -> log.error(t.getMessage(), t));
        }

        public CompletionConsumer(Consumer<T> consumer, Consumer<Throwable> failureConsumer) {
            this.successConsumer = consumer;
            this.failureConsumer = failureConsumer;
        }

        public static <T> CompletionConsumer<T> propagateTo(CompletableFuture<T> f) {
            return propagateTo(f, Function.identity());
        }

        public static <T,R> CompletionConsumer<T> propagateTo(CompletableFuture<R> f, Function<T,R> mapper) {
            return new CompletionConsumer<>(t -> f.complete(mapper.apply(t)), f::completeExceptionally);
        }

        public static <T> CompletionConsumer<T> onSuccess(Consumer<T> consumer) {
            return new CompletionConsumer<>(consumer);
        }

        public CompletionConsumer<T> onFailure(Consumer<Throwable> failureConsumer) {
            return new CompletionConsumer<>(successConsumer, failureConsumer);
        }

        @Override
        public void accept(T t, Throwable throwable) {
            Optional.ofNullable(throwable).ifPresentOrElse(failureConsumer, () -> tryCatch(() -> successConsumer.accept(t), failureConsumer));
        }
    }

    public static class CompletionHandler<T,U> implements BiFunction<T, Throwable,U> {

        private final Function<T,U> successHandler;
        private final Function<Throwable,U> failureHandler;

        public CompletionHandler(Function<T,U> f) {
            this(f, t -> {
                log.error(t.getMessage(), t);
                return null;
            });
        }

        public CompletionHandler(Function<T,U> successHandler, Function<Throwable,U> failureHandler) {
            this.successHandler = successHandler;
            this.failureHandler = failureHandler;
        }

        public static <T,U> CompletionHandler<T,U> onSuccess(Function<T,U> handler) {
            return new CompletionHandler<>(handler);
        }

        public CompletionHandler<T,U> onFailure(Function<Throwable,U> failureHandler) {
            return new CompletionHandler<>(successHandler, failureHandler);
        }

        @Override
        public U apply(T t, Throwable throwable) {
            return Optional.ofNullable(throwable)
                    .map(failureHandler)
                    .orElseGet(() -> successHandler.apply(t));
        }
    }
}
