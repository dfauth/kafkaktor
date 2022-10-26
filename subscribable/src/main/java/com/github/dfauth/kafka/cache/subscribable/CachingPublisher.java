package com.github.dfauth.kafka.cache.subscribable;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dfauth.kafka.cache.subscribable.CachingPredicate.duplicates;

public class CachingPublisher<T> implements Publisher<T> {

    private final Executor executor;
    private final List<SubscriptionContainer<T>> subscribers = new ArrayList<>();
    private final AtomicReference<T> ref = new AtomicReference<>();

    public CachingPublisher() {
        this(ForkJoinPool.commonPool());
    }

    public CachingPublisher(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        SubscriptionContainer<T> s = new SubscriptionContainer<>();
        Flux.from(s).filter(duplicates()).subscribe(subscriber);
        this.subscribers.add(s);
        publish();
    }

    public T publish(T t) {
        ref.set(t);
        publish();
        return t;
    }

    private void publish() {
        Optional.ofNullable(ref.get()).ifPresent(t ->  this.subscribers.forEach(s -> executor.execute(() -> s.onNext(t))));
    }

    public void start() {
    }

    public void stop() {
        Optional.ofNullable(ref.get()).ifPresent(t ->  this.subscribers.forEach(s -> executor.execute(s::onComplete)));
    }

    public void stop(Throwable throwable) {
        Optional.ofNullable(ref.get()).ifPresent(t ->  this.subscribers.forEach(s -> executor.execute(() -> s.onError(throwable))));
    }

    private static class SubscriptionContainer<T> implements Publisher<T>, Subscription {
        private Subscriber<? super T> subscriber;
        private final AtomicLong cnt = new AtomicLong(0);

        @Override
        public void request(long l) {
            cnt.set(l);
        }

        @Override
        public void cancel() {
            this.cnt.set(0);
            this.subscriber.onComplete();
        }

        public void onNext(T t) {
            if(cnt.decrementAndGet() >= 0) {
                this.subscriber.onNext(t);
            }
        }

        public void onComplete() {
            this.subscriber.onComplete();
        }

        public void onError(Throwable throwable) {
            this.subscriber.onError(throwable);
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(this);
        }
    }
}
