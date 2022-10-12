package com.github.dfauth.functional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Tuple2Test {

    Integer t1 = 1;
    String t2 = "A";

    @Test
    public void testOf() {
        Tuple2<Integer, String> t = Tuple2.tuple2(t1, t2);
        assertEquals(t1, t._1());
        assertEquals(t2, t._2());
    }

    @Test
    public void testForEach() {
        Tuple2<Integer, String> t = Tuple2.tuple2(t1, t2);
        t.forEach((_t1, _t2) -> {
            assertEquals(t1, t._1());
            assertEquals(t2, t._2());
        });
    }

    @Test
    public void testMap() {
        Tuple2<Integer, String> t = Tuple2.tuple2(t1, t2);
        assertEquals(t1, t.map((_t1, _t2) -> t1));
        assertEquals(t1, t.map((_t1, _t2) -> t1));
        assertEquals(t1, t.map(_t1 -> _t2 -> t1));
    }

}
