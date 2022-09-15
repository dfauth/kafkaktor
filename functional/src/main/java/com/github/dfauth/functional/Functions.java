package com.github.dfauth.functional;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static com.github.dfauth.trycatch.TryCatch.RunnableBuilder.ignoringConsumer;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;

public class Functions {

    public static <T> Predicate<T> toPredicate(Function<T, Boolean> f) {
        return f::apply;
    }

    public static <T> UnaryOperator<T> peek(Consumer<T> c) {
        return t -> {
            tryCatch(() -> c.accept(t), ignoringConsumer);
            return t;
        };
    }
}
