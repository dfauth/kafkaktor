package com.github.dfauth.trycatch;

import java.util.concurrent.Callable;
import java.util.function.Function;

import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;

public interface CallableFunction<T,R> extends Function<T, Callable<R>> {

    static <T,R> java.util.function.Function<T,R> toFunction(CallableFunction<T,R> f) {
        return t -> tryCatch(f.apply(t));
    }

    @Override
    default Callable<R> apply(T t) {
        return () -> _apply(t);
    }

    R _apply(T t) throws Exception;

}
