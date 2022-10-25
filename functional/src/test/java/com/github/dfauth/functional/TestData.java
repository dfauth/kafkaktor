package com.github.dfauth.functional;

interface TestData {

    static I doit0() {
        return new I();
    }
    
    static I doit1(A a) {
        return new I();
    }
    
    static I doit2(A a, B b) {
        return new I();
    }
    
    static I doit3(A a, B b, C c) {
        return new I();
    }
    
    static I doit4(A a, B b, C c, D d) {
        return new I();
    }
    
    static I doit5(A a, B b, C c, D d, E e) {
        return new I();
    }
    
    static I doit6(A a, B b, C c, D d, E e,F f) {
        return new I();
    }
    
    static I doit7(A a, B b, C c, D d, E e,F f,G g) {
        return new I();
    }
    
    static I doit8(A a, B b, C c, D d, E e,F f,G g,H h) {
        return new I();
    }

    class A {}
    
    class B {}
    
    class C {}
    
    class D {}
    
    class E {}
    
    class F {}
    
    class G {}
    
    class H {}
    
    class I {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof I;
        }
    }

    static int testInt4(int a, int b, int c, int d) {
        return a+b+c+d;
    }
    
    static int testInt5(int a, int b, int c, int d, int e) {
        return a+b+c+d+e;
    }
    
    static int testInt6(int a, int b, int c, int d, int e, int f) {
        return a+b+c+d+e+f;
    }
    
    static int testInt7(int a, int b, int c, int d, int e, int f, int g) {
        return a+b+c+d+e+f+g;
    }
    
    static int testInt8(int a, int b, int c, int d, int e, int f, int g, int h) {
        return a+b+c+d+e+f+g+h;
    }
}
