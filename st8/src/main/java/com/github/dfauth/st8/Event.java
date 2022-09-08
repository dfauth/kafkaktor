package com.github.dfauth.st8;

import java.util.Optional;

public interface Event<T,X> {

    static <T,X> Builder<T,X> builder() {
        return new Builder();
    }

    T type();

    default Optional<X> payload() {
        return Optional.empty();
    }

    static class Builder<T,X> {

        private T type;
        private Optional<X> payload = Optional.empty();

        public Builder<T, X> withType(T type) {
            this.type = type;
            return this;
        }

        public Builder<T,X> withPayload(X payload) {
            this.payload = Optional.ofNullable(payload);
            return this;
        }

        public Event<T,X> build() {
            return new Event<>() {
                @Override
                public T type() {
                    return type;
                }

                @Override
                public Optional<X> payload() {
                    return payload;
                }
            };
        }
    }
}
