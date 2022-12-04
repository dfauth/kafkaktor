package com.github.dfauth.functional;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@FunctionalInterface
public interface Function1<A, B> extends Function<A, B> {

    static <A, B> Function1<A, B> function1(Function<A, B> f) {
        return f::apply;
    }

    static <A> Function1<A, Void> function1(Consumer<A> f) {
        return a -> {
            f.accept(a);
            return null;
        };
    }

    static <A,B> Function1<A, B> function1(Runnable r) {
        return a -> {
            r.run();
            return null;
        };
    }

    static <A, B> Consumer<A> consumer(Function<A, B> f) {
        return f::apply;
    }

    static <A> Predicate<A> toPredicate(Function<A, Boolean> f) {
        return f::apply;
    }

    default Consumer<A> toConsumer() {
        return consumer(this);
    }

    default Predicate<A> toPredicate() {
        return Function1.toPredicate((Function<A, Boolean>) this);
    }

    interface VoidFunction1<T> extends Function1<T,Void>, Consumer<T> {

        static <T> VoidFunction1<T> function1(Runnable r) {
            return _ignored -> {
                r.run();
                return null;
            };
        }

        @Override
        default void accept(T t) {
            apply(t);
        }
    }
}
