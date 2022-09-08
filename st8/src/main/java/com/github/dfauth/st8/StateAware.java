package com.github.dfauth.st8;

public interface StateAware<R,T,U,V,W,X> {

    R withState(State<T,U,V,W,X> s);
}
