package com.github.dfauth.functional;

import java.util.function.Consumer;

public interface Either<L,R> {
    L left();
    R right();

    default boolean isLeft() {
        return false;
    }

    default boolean isRight() {
        return false;
    }

    static <L, R> Either<L,R> left(L l) {
        return new Left<>(l);
    }

    static <L, R> Either<L,R> right(R r) {
        return new Right<>(r);
    }

    default Either<L, R> onLeft(Consumer<L> l) {
        return this;
    }

    default Either<L, R> onRight(Consumer<R> r) {
        return this;
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

        @Override
        public R right() {
            throw new IllegalStateException("Cannot invoke right() on Left");
        }

        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public Either<L, R> onLeft(Consumer<L> l) {
            l.accept(target);
            return this;
        }
    }

    class Right<L,R> implements Either<L,R> {

        private final R target;

        Right(R target) {
            this.target = target;
        }

        @Override
        public L left() {
            throw new IllegalStateException("Cannot invoke left() on Right");
        }

        @Override
        public R right() {
            return target;
        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public Either<L, R> onRight(Consumer<R> r) {
            r.accept(target);
            return this;
        }
    }
}
