package com.github.dfauth.functional;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.github.dfauth.functional.Lists.*;
import static org.junit.Assert.assertEquals;

public class ListsTest {

    private static final List<Integer> REF = List.of(1,2,3,4,5,6,7,8);
    private static final List<Integer> LEFT = List.of(2,4,6,8);
    private static final List<Integer> RIGHT = List.of(1,3,5,7);
    private static final List<Integer> TOP = List.of(1,2,3,4);
    private static final List<Integer> BOTTOM = List.of(5,6,7,8);

    @Test
    public void testThis() {
        assertEquals(1, (int)head(REF));
        assertEquals(Optional.of(1), headOption(REF));
        assertEquals(REF.subList(1, REF.size()), tail(REF));
        assertEquals(Tuple2.of(LEFT,RIGHT), partition(REF, i -> i%2==0));
    }

    @Test
    public void testThat() {
        assertEquals(Tuple2.of(LEFT, RIGHT), extendedList(REF).partition(i -> i%2==0));
        assertEquals(1, (int)extendedList(REF).head());
        assertEquals(Optional.of(1), extendedList(REF).headOption());
        assertEquals(List.of(2,3,4,5,6,7,8), extendedList(REF).tail());
        assertEquals(Tuple2.of(1, List.of(2,3,4,5,6,7,8)), extendedList(REF).segment());
        assertEquals(REF, extendedList(TOP).concat(5,6,7,8));
        assertEquals(List.of(4,3,2,1), extendedList(TOP).reverse());
        assertEquals(REF, extendedList(TOP).append(BOTTOM));
    }

}
