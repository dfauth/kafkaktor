package com.github.dfauth.st8;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Transition<T,U,V,W,X> extends Predicate<U>, BiFunction<Event<V,X>, U,State<T,U,V,W,X>> {

    static <U,T,V,W,X> Builder<T,U,V,W,X> newBuilder(State.Builder<T,U,V,W,X> parent, V event) {
        return new Builder<>(parent, event);
    }

    State<T,U,V,W,X> startingState();

    Event<V,X> event();

    boolean guard(U ctx);

    State<T,U,V,W,X> onEvent(Event<V,X> e, U ctx);

    @Override
    default boolean test(U ctx) {
        return guard(ctx);
    }

    @Override
    default State<T,U,V,W,X> apply(Event<V,X> e, U ctx) {
        return onEvent(e, ctx);
    }

    @Slf4j
    class Builder<T,U,V,W,X> {

        private final State.Builder<T,U,V,W,X> parent;
        private final Event<V,X> event;
        private Predicate<U> guard = ignored -> true;
        private T next;
        private TransitionListener<T,U,V,W,X> onTransition;

        public Builder(State.Builder<T,U,V,W,X> parent, V e) {
            this.parent = parent;
            this.event = () -> e;
        }

        public Builder<T,U,V,W,X> unless(Predicate<U> guard) {
            this.guard = guard;
            return this;
        }

        public Builder<T,U,V,W,X> goTo(T t) {
            this.next = t;
            return this;
        }

        public State.Builder<T,U,V,W,X> state(T t) {
            return parent.addTransition(this).state(t);
        }

        Transition<T,U,V,W,X> build(State<T,U,V,W,X> current) {
            State.Builder<T, U, V, W, X> builder = parent.resolve(next);
            return new Transition<>() {

                @Override
                public State<T,U,V,W,X> startingState() {
                    return current;
                }

                public Event<V,X> event() {
                    return event;
                }

                @Override
                public boolean guard(U ctx) {
                    return guard.test(ctx);
                }

                @Override
                public State<T,U,V,W,X> onEvent(Event<V,X> e, U ctx) {
                    Function<T, Optional<W>> x = onTransition.withState(startingState())
                            .withEvent(e)
                            .withStateMachineContext(ctx);
                    return builder.build(x);
                }
            };
        }

        public Builder<T,U,V,W,X> onTransition(TransitionListener<T,U,V,W,X> listener) {
            onTransition = listener;
            return this;
        }

        public Builder<T,U,V,W,X> onEvent(V e) {
            return parent.addTransition(this).onEvent(e);
        }
    }
}
