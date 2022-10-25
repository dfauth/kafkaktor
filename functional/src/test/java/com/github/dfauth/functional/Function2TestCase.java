package com.github.dfauth.functional;

import org.junit.Test;

import java.util.function.BiFunction;
import java.util.function.Function;

import static com.github.dfauth.functional.Function2.function2;
import static org.junit.Assert.*;

public class Function2TestCase implements TestData {

    private static final BiFunction<Integer, Integer, String> REF = (i, j) -> String.format("%d + %d = %d",i,j,(i + j));

    @Test
    public void testThis() {
        Function2<Integer, Integer, String> f = function2(REF);
        int i =1, j = 2;
        assertEquals(REF.apply(i,j), f.unwind().apply(i).apply(j));
        assertEquals(REF.apply(i,j), f.curry().apply(i).apply(j));
        assertEquals(REF.apply(i,j), f.curryLeft().apply(i).apply(j));
        assertNotEquals(REF.apply(i,j), f.curryRight().apply(i).apply(j));
        assertEquals(REF.apply(i,j), f.flip().curryRight().apply(i).apply(j));
    }

    @Test
    public void testThat() {
        BiFunction<A, B, I> f = TestData::doit2;
        Function2<A, B, I> f1 = function2(f);
        Function<A, Function<B, I>> f2 = f1.curry();
        Function<A, Function<B, I>> f3 = f1.unwind();
        assertNotNull(f2.apply(new A()).apply(new B()));
        assertNotNull(f3.apply(new A()).apply(new B()));
    }

}
