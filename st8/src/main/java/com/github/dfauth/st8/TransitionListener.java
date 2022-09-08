package com.github.dfauth.st8;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface TransitionListener<T,U,V,W,X> extends StateAware<EventAware<StateMachineContextAware<Function<T, Optional<W>>,U>,V,X>,T,U,V,W,X> {

    static <T,U,V,W,X> TransitionListener<T,U,V,W,X> sourceAndDestinationStates(BiFunction<State<T,U,V,W,X>,T,Optional<W>> f) {
        return t -> r -> u -> v -> f.apply(t,v);
    }

    static <T,U,V,W,X> TransitionListener<T,U,V,W,X> stateMachineContext(Consumer<U> f) {
        return t -> r -> u -> v -> {
            f.accept(u);
            return t.payload;
        };
    }

    static <T,U,V,W,X> TransitionListener<T,U,V,W,X> contextAndEvent(BiConsumer<Event<V,X>, U> f) {
        return t -> r -> u -> v -> {
            f.accept(r,u);
            return t.payload;
        };
    }

    static <T,U,V,W,X> TransitionListener<T,U,V,W,X> eventPayload(Function<Event<V,X>,Optional<W>> f) {
        return t -> r -> u -> v -> f.apply(r);

    }
}
