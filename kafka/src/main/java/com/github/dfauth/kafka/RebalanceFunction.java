package com.github.dfauth.kafka;

import java.util.function.Consumer;
import java.util.function.Function;

public interface RebalanceFunction<K,V,T,U> extends KafkaConsumerAware<K,V,Function<T,U>> {

    static <K,V,T> KafkaConsumerAware<K,V,Function<T,T>> fromConsumer(KafkaConsumerAware<K,V, Consumer<T>> consumer) {
        return c -> t -> {
            consumer.withKafkaConsumer(c).accept(t);
            return t;
        };
    }

    default <R> RebalanceFunction<K,V,T,R> andThen(RebalanceFunction<K,V,U,R> f) {
        return c -> withKafkaConsumer(c).andThen(f.withKafkaConsumer(c));
    }

    default <R> RebalanceFunction<K,V,R,U> compose(RebalanceFunction<K,V,R,T> f) {
        return f.andThen(this);
    }
}
