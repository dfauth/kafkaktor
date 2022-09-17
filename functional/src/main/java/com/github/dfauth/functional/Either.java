package com.github.dfauth.functional;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Either<L,R> {
    default L left() {
        throw new IllegalStateException("Cannot invoke left() on Right");
    }

    default R right() {
        throw new IllegalStateException("Cannot invoke right() on Left");
    }

    default boolean isLeft() {
        return getClass() == Left.class;
    }

    default boolean isRight() {
        return getClass() == Right.class;
    }

    static <L, R> Either<L,R> createLeft(L l) {
        return new Left<>(l);
    }

    static <L, R> Either<L,R> createRight(R r) {
        return new Right<>(r);
    }

    default void acceptLeft(Consumer<L> c) {
        Optional.of(this).filter(Either::isLeft).map(Either::left).ifPresent(c);
    }

    default void acceptRight(Consumer<R> c) {
        Optional.of(this).filter(Either::isRight).map(Either::right).ifPresent(c);
    }

    default <T> Optional<T> mapRight(Function<R,T> f) {
        return Optional.of(this).filter(Either::isRight).map(Either::right).map(f);
    }

    default <T> Optional<T> mapLeft(Function<L,T> f) {
        return Optional.of(this).filter(Either::isLeft).map(Either::left).map(f);
    }

    class Left<L,R> implements Either<L,R> {

        private final L target;

        Left(L target) {
            this.target = target;
        }

        @Override
        public L left() {
            return target;
        }
    }

    class Right<L,R> implements Either<L,R> {

        private final R target;

        Right(R target) {
            this.target = target;
        }

        @Override
        public R right() {
            return target;
        }
    }
}
