package com.github.dfauth.functional;

import org.junit.Test;

import java.util.function.BiFunction;

import static com.github.dfauth.functional.Function2.asFunction2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class Function2TestCase {

    private static final BiFunction<Integer, Integer, String> REF = (i, j) -> String.format("%d + %d = %d",i,j,(i + j));

    @Test
    public void testIt() {
        Function2<Integer, Integer, String> f = asFunction2(REF);
        int i =1, j = 2;
        assertEquals(REF.apply(i,j), f.curried().apply(i).apply(j));
        assertEquals(REF.apply(i,j), f.curriedLeft().apply(i).apply(j));
        assertNotEquals(REF.apply(i,j), f.curriedRight().apply(i).apply(j));
        assertEquals(REF.apply(i,j), f.flip().curriedRight().apply(i).apply(j));
    }

}
