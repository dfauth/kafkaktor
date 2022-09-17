package com.github.dfauth.st8;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;

public class StateMachine<T,U,V,W,X> {

    private State<T,U,V,W,X> current;

    public StateMachine(State<T,U,V,W,X> initialState) {
        current = initialState;
    }

    public static <T,U,V,W,X> Builder<T,U,V,W,X> create(String name, U ctx) {
        return new Builder<>(name, ctx);
    }

    public State<T,U,V,W,X> currentState() {
        return current;
    }

    public StateMachine<T,U,V,W,X> onEvent(Event<V,X> e, U ctx) {
        current = current.onEvent(e, ctx).orElse(current);
        return this;
    }

    @Slf4j
    public static class Builder<T,U,V,W,X> {

        private T initial;
        private final Map<T, State.Builder<T,U,V,W,X>> stateBuilders = new HashMap<>();
        private State<T,U,V,W,X> initialState;

        public Builder(String name, U ctx) {
        }

        public State.Builder<T,U,V,W,X> initial(T t) {
            initial = t;
            return State.newBuilder(this, t);
        }

        public Builder<T,U,V,W,X> onEntry(Consumer<State<T,U,V,W,X>> consumer) {
            return this;
        }

        public Builder<T,U,V,W,X> addState(State.Builder<T,U,V,W,X> state) {
            this.stateBuilders.put(state.nested, state);
            return this;
        }

        public State.Builder<T,U,V,W,X> newBuilder(T t) {
            return State.newBuilder(this, t);
        }

        public StateMachine<T,U,V,W,X> build() {
            return tryCatch(() -> {
                this.stateBuilders.values().forEach(b -> {
                    State<T,U,V,W,X> s = b.build();
                    if(s.type == initial) {
                        initialState = s;
                    }
                });
                return new StateMachine<>(initialState);
            });
        }

        public State.Builder<T, U, V, W, X> resolve(T t) {

            return Optional.ofNullable(stateBuilders.get(t))
                    .orElseThrow(() -> new IllegalArgumentException("No state associated with value "+t));
        }

        public Optional<State.Builder<T,U,V,W,X>> state(T t) {
            return Optional.ofNullable(stateBuilders.get(t));
        }
    }

}