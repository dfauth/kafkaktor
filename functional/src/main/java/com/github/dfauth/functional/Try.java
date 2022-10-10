package com.github.dfauth.functional;

import com.github.dfauth.trycatch.DispatchHandler;
import com.github.dfauth.trycatch.ExceptionalRunnable;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.dfauth.trycatch.TryCatch.CallableBuilder.loggingOperator;
import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;

public interface Try<T> {

    static <T> Try<T> tryWithCallable(Callable<T> c) {
        return tryCatch(() -> new Success<>(c.call()), loggingOperator.andThen(Failure::new));
    }

    static <T> Try<T> trySilentlyWithCallable(Callable<T> c) {
        return tryCatch(() -> new Success<>(c.call()), Failure::new);
    }

    static Try<Void> tryWith(ExceptionalRunnable r) {
        return tryCatch(() -> {
            r.run();
            return new Success<>(Unit.UNIT);
        }, Failure::new);
    }

    static <T> BiFunction<T,Throwable,Try<T>> tryWith() {
        return (t,e) ->
                t != null ?
                        new Success<>(t) :
                        e != null ?
                                new Failure<>(e) :
                                new Failure<>(new UnsupportedOperationException());
    }

    static <T> Failure<T> failure(Throwable t) {
        return new Failure<>(t);
    }

    static <T> Success<T> success(T t) {
        return new Success<>(t);
    }

    static <T> Failure<T> toFailure(Try<T> t) {
        return t.dispatch(new DispatchHandler<>() {
            @Override
            public Failure<T> dispatch(Failure<T> f) {
                return f;
            }

            @Override
            public Failure<T> dispatch(Success<T> s) {
                throw new UnsupportedOperationException();
            }
        });
    }

    static <T> Success<T> toSuccess(Try<T> t) {
        return t.dispatch(new DispatchHandler<>() {
            @Override
            public Success<T> dispatch(Failure<T> f) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Success<T> dispatch(Success<T> s) {
                return s;
            }
        });
    }

    Try<T> recover(Function<Throwable,T> f);

    <V> V dispatch(DispatchHandler<T,V> handler);

    <R> Try<R> map(Function<T,R> f);

    Try<T> accept(Consumer<T> c);

    <R> Try<R> flatMap(Function<T,Try<R>> f);

    Optional<T> toOptional();

    boolean isFailure();

    boolean isSuccess();

    default Failure<T> toFailure() throws ClassCastException {
        return toFailure(this);
    }

    default Success<T> toSuccess()  throws ClassCastException {
        return toSuccess(this);
    }
}
