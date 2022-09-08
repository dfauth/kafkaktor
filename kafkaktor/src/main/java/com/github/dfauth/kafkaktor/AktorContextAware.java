package com.github.dfauth.kafkaktor;

public interface AktorContextAware<T> {

    T withAktorContext(AktorContext ktx);
}
