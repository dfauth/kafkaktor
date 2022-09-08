package com.github.dfauth.st8;

public interface EventAware<T,R,X> {

    T withEvent(Event<R,X> e);
}
