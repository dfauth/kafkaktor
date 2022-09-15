package com.github.dfauth.functional;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.github.dfauth.functional.Lists.*;
import static org.junit.Assert.assertEquals;

public class ListsTest {

    private static final List<Integer> REF = List.of(1,2,3,4,5,6,7,8);

    @Test
    public void testIt() {
        assertEquals(1, (int)head(REF));
        assertEquals(Optional.of(1), headOption(REF));
        assertEquals(REF.subList(1, REF.size()), tail(REF));
        assertEquals(Tuple2.of(List.of(2,4,6,8), List.of(1,3,5,7)), partition(REF, i -> i%2==0));
    }

}
