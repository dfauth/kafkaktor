package com.github.dfauth.trycatch;

import com.github.dfauth.functional.Failure;
import com.github.dfauth.functional.Success;
import com.github.dfauth.functional.Unit;

import static com.github.dfauth.trycatch.TryCatch.CallableBuilder.loggingOperator;

public interface DispatchHandler<T, R> {

    R dispatch(Failure<T> f);

    R dispatch(Success<T> s);

    interface Consumer<T> extends DispatchHandler<T, Unit>, java.util.function.Consumer<T> {

        static <T> Consumer<T> toDispatcher(java.util.function.Consumer<T> consumer) {
            return t -> consumer.accept(t);
        }

        default Unit dispatch(Failure<T> f) {
            loggingOperator.apply(f.exception());
            return Unit.UNIT;
        }

        default Unit dispatch(Success<T> s) {
            accept(s.result());
            return Unit.UNIT;
        }
    }

    interface Function<T,R> extends DispatchHandler<T, R>, java.util.function.Function<T,R> {

        static <T,R> Function<T,R> toDespatcher(java.util.function.Function<T,R> f) {
            return f::apply;
        }

        static <T> Function<T,T> extract() {
            return toDespatcher(java.util.function.Function.identity());
        }

        default R dispatch(Failure<T> f) {
            loggingOperator.apply(f.exception());
            throw new RuntimeException(f.exception());
        }

        default R dispatch(Success<T> s) {
            return apply(s.result());
        }
    }
}
