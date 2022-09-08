package com.github.dfauth.st8;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class State<T,U,V,W,X> {

    final T type;
    final Consumer<U> onEntryConsumer;
    final Consumer<U> onExitConsumer;
    final List<Transition<T,U,V,W,X>> transitions;
    final Optional<W> payload;

    public State(T type, Consumer<U> onEntryConsumer, Consumer<U> onExitConsumer, List<Transition.Builder<T,U,V,W,X>> transitionBuilders) {
        this(type, onEntryConsumer, onExitConsumer, transitionBuilders, Optional.empty());
    }
    public State(T type, Consumer<U> onEntryConsumer, Consumer<U> onExitConsumer, List<Transition.Builder<T,U,V,W,X>> transitionBuilders, Optional<W> payload) {
        this.type = type;
        this.onEntryConsumer = onEntryConsumer;
        this.onExitConsumer = onExitConsumer;
        this.transitions = transitionBuilders.stream().map(b -> b.build(State.this)).collect(Collectors.toList());
        this.payload = payload;
    }

    public static <T,U,V,W,X> Builder<T,U,V,W,X> newBuilder(StateMachine.Builder<T,U,V,W,X> parent, T t) {
        Builder<T,U,V,W,X> b = new Builder<>(parent, t);
        parent.addState(b);
        return b;
    }

    public T type() {
        return type;
    }

    public boolean isFinal() {
        return transitions.size() == 0;
    }

    public Optional<State<T,U,V,W,X>> onEvent(Event<V,X> e, U ctx) {
        return transitions.stream()
                .filter(t -> t.event().type().equals(e.type()))
                .filter(t -> t.guard(ctx))
                .map(t -> {
                    this.onExit(ctx);
                    State<T,U,V,W,X> s = t.onEvent(e, ctx);
                    s.onEntry(ctx);
                    return s;
                })
                .findFirst();
    }

    public void onEntry(U ctx) {
        onEntryConsumer.accept(ctx);
    }

    public void onExit(U ctx) {
        onExitConsumer.accept(ctx);
    }

    public Optional<W> payload() {
        return payload;
    }

    public static class Builder<T,U,V,W,X> {

        final T nested;
        final StateMachine.Builder<T,U,V,W,X> parent;
        Consumer<U> onEntry = u -> {};
        Consumer<U> onExit = u -> {};
        List<Transition.Builder<T,U,V,W,X>> transitionBuilders = new ArrayList<>();

        public Builder(StateMachine.Builder<T,U,V,W,X> parent, T t) {
            this.parent = parent;
            this.nested = t;
        }

        public Builder<T,U,V,W,X> onEntry(Consumer<U> consumer) {
            this.onEntry = consumer;
            return this;
        }

        public Builder<T,U,V,W,X> onExit(Consumer<U> consumer) {
            this.onExit = consumer;
            return this;
        }

        public Transition.Builder<T,U,V,W,X> onEvent(V event) {
            return Transition.newBuilder(this, event);
        }

        public Builder<T,U,V,W,X> addTransition(Transition.Builder<T,U,V,W,X> transitionBuilder) {
            transitionBuilders.add(transitionBuilder);
            return this;
        }

        public Builder<T,U,V,W,X> addTransitions(Collection<Transition.Builder<T,U,V,W,X>> transitions) {
            transitionBuilders.addAll(transitions);
            return this;
        }

        public Builder<T,U,V,W,X> state(T t) {
            return newBuilder(parent, t);
        }

        State<T,U,V,W,X> build() {
            return build(Optional.empty());
        }

        State<T,U,V,W,X> build(Optional<W> payload) {
            return new State<>(nested,
                    onEntry,
                    onExit,
                    transitionBuilders,
                    payload);
        }

        State<T,U,V,W,X> build(Function<T,Optional<W>> f) {
            return new State(nested,
                    onEntry,
                    onExit,
                    transitionBuilders,
                    f.apply(nested));
        }

        public Builder<T, U, V, W, X> resolve(T next) {
            return parent.resolve(next);
        }
    }
}
