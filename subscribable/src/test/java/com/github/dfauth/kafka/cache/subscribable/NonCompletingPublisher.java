package com.github.dfauth.kafka.cache.subscribable;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.util.stream.Stream;

public class NonCompletingPublisher<T> {

    @SafeVarargs
    public static <T> Publisher<T> supplyAndComplete(T... ts) {
        return supply(true, ts);
    }

    @SafeVarargs
    public static <T> Publisher<T> supply(T... ts) {
        return supply(false, ts);
    }

    @SafeVarargs
    public static <T> Publisher<T> supply(boolean doComplete, T... ts) {
        return subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long l) {}

                @Override
                public void cancel() {}
            });
            Stream.of(ts).forEach(subscriber::onNext);
            if(doComplete) {
                subscriber.onComplete();
            }
        };
    }
}
