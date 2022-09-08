package com.github.dfauth.kafka;

public interface KeyAware<T> {
    T withKey(String key);
}
