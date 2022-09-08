package com.github.dfauth.st8;

public interface StateMachineContextAware<T,U> {

    T withStateMachineContext(U u);
}
