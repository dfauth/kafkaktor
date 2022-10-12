package com.github.dfauth.functional;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public interface Lists<T> extends List<T> {

    static <T> Optional<T> headOption(List<T> l) {
        return Optional.ofNullable(l.size() > 0 ? l.get(0) : null);
    }

    static <T> T head(List<T> l) {
        return headOption(l).orElseThrow();
    }

    static <T> List<T> tail(List<T> l) {
        return l.size() > 1 ? List.copyOf(l.subList(1,l.size())) : emptyList();
    }

    static <T> Tuple2<List<T>,List<T>> segment(List<T> l) {
        return segment(l, t -> t == head(l));
    }

    static <T> Tuple2<List<T>,List<T>> segment(List<T> l, Predicate<T> p) {
        int n = IntStream.range(0 ,l.size()).filter(i -> p.negate().test(l.get(i))).findFirst().orElse(0);
        return Tuple2.tuple2(l.subList(0, n), l.subList(n, l.size()));
    }

    static <T> Tuple2<List<T>,List<T>> partition(List<T> l, Predicate<T> p) {
        BiFunction<Tuple2<List<T>, List<T>>, Either<T, T>, Tuple2<List<T>, List<T>>> f = (_t, _e) ->
            _t.map((t1,t2) ->
                _e.mapLeft(_l -> concat(t1, _l))
                        .map(_t1 -> Tuple2.tuple2(_t1, t2))
                        .orElseGet(() -> Tuple2.tuple2(t1, _e.mapRight(_r -> concat(t2, _r))
                                .orElseThrow(() -> new IllegalStateException("Either is neither left nor right - should never happen"))))
            );
        return l.stream()
                .map(e -> Optional.ofNullable(e).filter(p).map(Either::<T, T>createLeft).orElseGet(() -> Either.createRight(e)))
                .reduce(Tuple2.tuple2(emptyList(), emptyList()),
                        f,
                        (t1, t2) -> Tuple2.tuple2(concat(t1._1(), t2._1()),concat(t1._2(), t2._2()))
                );
    }

    @SafeVarargs
    static <T> List<T> concat(List<T>... lists) {
        return Stream.of(lists).reduce(new ArrayList<>(), (acc, l) -> {
            acc.addAll(l);
            return acc;
        }, (acc1, acc2) -> {
            acc1.addAll(acc2);
            return acc1;
        });
    }

    static <T> List<T> reverse(List<T> l) {
        List<T> tmp = new ArrayList<>(l);
        Collections.reverse(tmp);
        return List.copyOf(tmp);
    }

    @SafeVarargs
    static <T> List<T> concat(List<T> l, T... ts) {
        List<T> tmp = new ArrayList<>(l);
        tmp.addAll(Arrays.asList(ts));
        return List.copyOf(tmp);
    }

    static <T,R> R foldLeft(List<T> l, R r, BiFunction<R,T,R> f) {
        return l.stream().reduce(r, f, (r1, r2) -> {
            throw new IllegalStateException("should never happen");
        });
    }

    static <T,R> R foldLeft(List<T> l, R r, BiFunction<R,T,R> f, BinaryOperator<R> g) {
        return l.parallelStream().reduce(r, f, g);
    }

    static <T> ExtendedList<T> extendedList() {
        return extendedList(new ArrayList<>());
    }

    static <T> ExtendedList<T> extendedList(Collection<T> l) {
        return new ExtendedList<>(l);
    }

    default Optional<T> headOption() {
        return Optional.ofNullable(size() > 0 ? get(0) : null);
    }

    default T head() {
        return headOption().orElseThrow();
    }

    default List<T> tail() {
        return size() > 1 ? List.copyOf(subList(1,size())) : emptyList();
    }

    default Tuple2<List<T>, List<T>> partition(Predicate<T> p) {
        return Lists.partition(this, p);
    }

    default Tuple2<List<T>,List<T>> segment() {
        return Lists.segment(this);
    }

    default Tuple2<List<T>,List<T>> segment(Predicate<T> p) {
        return Lists.segment(this, p);
    }

    default ExtendedList<T> reverse() {
        return extendedList(reverse(this));
    }

    default List<T> append(List<T> l) {
        return concat(this, l);
    }

    default List<T> concat(T... ts) {
        return concat(this, ts);
    }

    default <R> R foldLeft(R r, BiFunction<R,T,R> f) {
        return foldLeft(this, r, f);
    }

    default <R> R foldLeft(R r, BiFunction<R,T,R> f, BinaryOperator<R> g) {
        return foldLeft(this, r, f, g);
    }

    class ExtendedList<T> extends ArrayList<T> implements Lists<T> {

        public ExtendedList(Collection<T> l) {
            super(l);
        }
    }
}
