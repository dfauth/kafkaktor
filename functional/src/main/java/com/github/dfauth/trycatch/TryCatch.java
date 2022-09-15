package com.github.dfauth.trycatch;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static com.github.dfauth.functional.Functions.peek;
import static com.github.dfauth.trycatch.ExceptionalRunnable.asExceptionalRunnable;
import static com.github.dfauth.trycatch.TryCatch.CallableBuilder.*;

@Slf4j
public abstract class TryCatch {

    public static abstract class _Runnable {

        public static void tryCatch(Runnable r) {
            tryCatch(asExceptionalRunnable(r));
        }

        public static void tryCatch(ExceptionalRunnable r) {
            tryCatch(r, RunnableBuilder.loggingConsumer.andThen(RunnableBuilder.propagationConsumer), Builder.noOpRunnable);
        }

        public static void tryCatch(ExceptionalRunnable er, Consumer<Throwable> c) {
            tryCatch(er, c, noOpRunnable);
        }

        public static void tryCatch(ExceptionalRunnable er, Runnable r) {
            tryCatch(er, RunnableBuilder.loggingConsumer, r);
        }

        public static void tryCatch(ExceptionalRunnable er, Consumer<Throwable> c, Runnable r) {
            Builder.tryCatch(er).handleWith(c).andFinallyRun(r);
        }

        public static void tryCatchIgnore(ExceptionalRunnable r) {
            tryCatch(r, RunnableBuilder.loggingConsumer);
        }

        public static <T> Consumer<T> withExceptionLogging(Consumer<T> c) {
            return t -> withExceptionLogging(() -> c.accept(t)).run();
        }

        public static Runnable withExceptionLogging(ExceptionalRunnable r) {
            return () -> tryCatch(r::run, RunnableBuilder.loggingConsumer.andThen(RunnableBuilder.propagationConsumer));
        }
    }

    public static abstract class _Callable {

        public static <T> T tryCatch(Callable<T> c) {
            return tryCatch(c, loggingOperator.andThen(CallableBuilder.propagationOperator()), Builder.noOpRunnable);
        }

        public static <T> T tryCatch(Callable<T> c, Function<Throwable, T> f) {
            return tryCatch(c, f, noOpRunnable);
        }

        public static <T> T tryCatch(Callable<T> c, Runnable r) {
            return tryCatch(c, loggingOperator.andThen(propagationOperator()), r);
        }

        public static <T> T tryCatch(Callable<T> c, Function<Throwable, T> f, Runnable r) {
            return Builder.tryCatch(c).handleWith(f).andFinallyRun(r);
        }

        public static <T> T tryCatchIgnore(Callable<T> c, T t) {
            return tryCatch(c, loggingOperator.andThen(CallableBuilder.defaultValueOf(t)), Builder.noOpRunnable);
        }

        public static <T,R> Function<T,R> withExceptionLogging(CallableFunction<T,R> f) {
            return t -> tryCatch(() -> f.apply(t).call());
        }

        public static <T,R> Function<T,R> withExceptionLogging(CallableFunction<T,R> f, R r) {
            return t -> tryCatch(() -> f.apply(t).call(), loggingOperator.andThen(defaultValueOf(r)));
        }

        public static <T> Callable<T> withExceptionLogging(Callable<T> c) {
            return () -> tryCatch(c);
        }
    }

    public static abstract class Builder {

        public static Runnable noOpRunnable = () -> {};

        protected Runnable finallyRunnable = noOpRunnable;

        public static RunnableBuilder tryCatch(Runnable r) {
            return tryCatch(asExceptionalRunnable(r));
        }

        public static RunnableBuilder tryCatch(ExceptionalRunnable r) {
            return new RunnableBuilder(r);
        }

        public static <T> CallableBuilder<T> tryCatch(Callable<T> r) {
            return new CallableBuilder<>(r);
        }
    }

    public static class RunnableBuilder extends Builder {

        public static Consumer<Throwable> loggingConsumer = e -> log.error(e.getMessage(), e);

        public static Consumer<Throwable> ignoringConsumer = e -> {};

        public static Consumer<Throwable> propagationConsumer = e -> {
            throw new RuntimeException(e);
        };

        private final ExceptionalRunnable payload;
        private Consumer<Throwable> exceptionHandler = loggingConsumer.andThen(propagationConsumer);

        public RunnableBuilder(ExceptionalRunnable r) {
            this.payload = r;
        }

        public RunnableBuilder handleWith(Consumer<Throwable> c) {
            this.exceptionHandler = c;
            return this;
        }

        public RunnableBuilder rethrow() {
            this.exceptionHandler = loggingConsumer.andThen(propagationConsumer);
            return this;
        }

        public RunnableBuilder ignore() {
            this.exceptionHandler = loggingConsumer;
            return this;
        }

        public RunnableBuilder ignoreSilently() {
            this.exceptionHandler = ignoringConsumer;
            return this;
        }

        public RunnableBuilder andFinally(Runnable finallyRunnable) {
            this.finallyRunnable = finallyRunnable;
            return this;
        }

        public void andFinallyRun(Runnable finallyRunnable) {
            andFinally(finallyRunnable).build();
        }


        public void rethrowAndRun() {
            rethrow().run();
        }

        public void ignoreAndRun() {
            ignore().run();
        }

        public void ignoreSilentlyAndRun() {
            ignoreSilently().run();
        }

        public void run() {
            build();
        }

        public void build() {
            try {
                payload._run();
            } catch (Throwable t) {
                exceptionHandler.accept(t);
            } finally {
                finallyRunnable.run();
            }
        }
    }

    public static class CallableBuilder<T> extends Builder {

        public static UnaryOperator<Throwable> loggingOperator = peek(e -> log.error(e.getMessage(), e));
        private final Callable<T> payload;
        private Function<Throwable, T> exceptionHandler = loggingOperator.andThen(propagationOperator());

        public CallableBuilder(Callable<T> r) {
            this.payload = r;
        }

        public static <T> Function<Throwable, T> propagationOperator() {
            return e -> {
                throw new RuntimeException(e);
            };
        }

        public static <T> Function<Throwable, T> defaultValueOf(T t) {
            return ignored -> t;
        }

        public CallableBuilder<T> handleWith(Function<Throwable, T> f) {
            this.exceptionHandler = f;
            return this;
        }

        public CallableBuilder<T> rethrow() {
            this.exceptionHandler = loggingOperator.andThen(propagationOperator());
            return this;
        }

        public CallableBuilder<T> andFinally(Runnable finallyRunnable) {
            this.finallyRunnable = finallyRunnable;
            return this;
        }

        public T andFinallyRun(Runnable finallyRunnable) {
            return andFinally(finallyRunnable).build();
        }

        public T rethrowAndRun() {
            return rethrow().run();
        }

        public T run() {
            return build();
        }

        public T build() {
            try {
                return payload.call();
            } catch (Throwable t) {
                return exceptionHandler.apply(t);
            } finally {
                finallyRunnable.run();
            }
        }

    }
}
