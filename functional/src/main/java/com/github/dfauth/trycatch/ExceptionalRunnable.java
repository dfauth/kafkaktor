package com.github.dfauth.trycatch;

import java.util.concurrent.Callable;

import static com.github.dfauth.functional.Unit.UNIT;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;

@FunctionalInterface
public interface ExceptionalRunnable extends Callable<Void>, Runnable {

    static ExceptionalRunnable asExceptionalRunnable(Runnable r) {
        return r::run;
    }

    static Runnable asRunnable(ExceptionalRunnable r) {
        return () -> tryCatch(r);
    }

    default Void call() throws Exception {
        _run();
        return UNIT;
    }

    default void run() {
        tryCatch(this);
    }

    void _run() throws Exception;

}
