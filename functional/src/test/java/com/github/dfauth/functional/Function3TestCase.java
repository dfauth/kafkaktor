package com.github.dfauth.functional;

import org.junit.Test;

import java.util.function.Function;

import static com.github.dfauth.functional.Function3.uncurry;
import static com.github.dfauth.functional.Function3.unwind;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Function3TestCase implements TestData {

    private static final Function3<Integer,Double,String,Double> REF = (i,j,k) -> i+j+Integer.parseInt(k);

    @Test
    public void testThis() {
        Function<Integer, Function<Double, Function<String, Double>>> f = REF.unwind();
        Function3<Integer, Double, String, Double> f1 = uncurry(f);
        int i =1; double j = 2.0; String k = "3";
        assertEquals(REF.apply(i,j,k), f.apply(i).apply(j).apply(k));
        assertEquals(REF.apply(i,j,k), f1.apply(i,j,k));
    }

    @Test
    public void testThat() {
        {
            Function3<A,B,C,I> f = TestData::doit3;
            Function<A, Function2<B, C, I>> f2 = f.curry();
            Function<A, Function<B, Function<C, I>>> f3 = f.unwind();
            assertNotNull(f3.apply(new A()).apply(new B()).apply(new C()));
        }
        {
            Function<A, Function<B, Function<C, I>>> f = unwind(TestData::doit3);
            assertEquals(new I(), f.apply(new A()).apply(new B()).apply(new C()));
        }
    }

}
