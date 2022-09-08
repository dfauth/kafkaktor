package com.github.dfauth.trycatch;

import com.github.dfauth.functional.Unit;

import java.util.concurrent.Callable;

import static com.github.dfauth.functional.Unit.UNIT;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;

@FunctionalInterface
public interface ExceptionalRunnable extends Callable<Unit>, Runnable {

    static ExceptionalRunnable asExceptionalRunnable(Runnable r) {
        return r::run;
    }

    static Runnable asRunnable(ExceptionalRunnable r) {
        return () -> tryCatch(r);
    }

    default Unit call() throws Exception {
        _run();
        return UNIT;
    }

    default void run() {
        tryCatch(() -> _run());
    }

    void _run() throws Exception;

}
