package com.github.dfauth.kafka.cache.subscribable;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Slf4j
public class CachingPredicate<T> implements Predicate<T> {

    public static <T> CachingPredicate<T> duplicates() {
        return cachingPredicate();
    }

    public static <T> CachingPredicate<T> cachingPredicate() {
        return new CachingPredicate<>();
    }

    private final AtomicReference<T> previous = new AtomicReference<>();

    @Override
    public boolean test(T t) {
        if(t.equals(previous.get())) {
            return false;
        } else {
            previous.set(t);
            return true;
        }
    }
}
