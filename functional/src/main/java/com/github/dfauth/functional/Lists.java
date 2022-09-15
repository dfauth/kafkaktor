package com.github.dfauth.functional;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Lists {

    public static <T> Optional<T> headOption(List<T> l) {
        return Optional.ofNullable(l.size() > 0 ? l.get(0) : null);
    }

    public static <T> T head(List<T> l) {
        return headOption(l).orElseThrow();
    }

    public static <T> List<T> tail(List<T> l) {
        return l.size() > 1 ? List.copyOf(l.subList(1,l.size())) : Collections.emptyList();
    }

    public static <T> Tuple2<T,List<T>> segment(List<T> l) {
        return Tuple2.of(head(l), tail(l));
    }

    public static <T> Tuple2<List<T>,List<T>> partition(List<T> l, Predicate<T> p) {
        return Tuple2.of(l.stream().filter(p).collect(Collectors.toList()), l.stream().filter(p.negate()).collect(Collectors.toList()));
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
