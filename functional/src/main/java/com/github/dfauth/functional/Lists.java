package com.github.dfauth.functional;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public class Lists {

    public static <T> Optional<T> headOption(List<T> l) {
        return Optional.ofNullable(l.size() > 0 ? l.get(0) : null);
    }

    public static <T> T head(List<T> l) {
        return headOption(l).orElseThrow();
    }

    public static <T> List<T> tail(List<T> l) {
        return l.size() > 1 ? List.copyOf(l.subList(1,l.size())) : emptyList();
    }

    public static <T> Tuple2<T,List<T>> segment(List<T> l) {
        return Tuple2.of(head(l), tail(l));
    }

    public static <T> Tuple2<List<T>,List<T>> partition(List<T> l, Predicate<T> p) {
        BiFunction<Tuple2<List<T>, List<T>>, Either<T, T>, Tuple2<List<T>, List<T>>> f = (_t, _e) -> {
            AtomicReference<Tuple2<List<T>, List<T>>> result = new AtomicReference<>(_t);
            _e.onLeft(_l -> result.set(_t.map((t1, t2) -> Tuple2.of(append(t1, _l), t2))))
                    .onRight(_r -> result.set(_t.map((t1, t2) -> Tuple2.of(t1, append(t2, _r)))));
            return result.get();
        };
        return l.stream()
                .map(e -> Optional.ofNullable(e).filter(p).map(Either::<T, T>left).orElseGet(() -> Either.right(e)))
                .reduce(Tuple2.of(emptyList(), emptyList()),
                        f,
                        (t1, t2) -> Tuple2.of(concat(t1._1(), t2._1()),concat(t1._2(), t2._2()))
                );
    }

    @SafeVarargs
    private static <T> List<T> concat(List<T>... lists) {
        return Stream.of(lists).reduce(new ArrayList<>(), (acc, l) -> {
            acc.addAll(l);
            return acc;
        }, (acc1, acc2) -> {
            acc1.addAll(acc2);
            return acc1;
        });
    }

    public static <T> List<T> reverse(List<T> l) {
        List<T> tmp = new ArrayList<>(l);
        Collections.reverse(tmp);
        return List.copyOf(tmp);
    }

    @SafeVarargs
    public static <T> List<T> append(List<T> l, T... ts) {
        List<T> tmp = new ArrayList<>(l);
        tmp.addAll(Arrays.asList(ts));
        return List.copyOf(tmp);
    }

}
