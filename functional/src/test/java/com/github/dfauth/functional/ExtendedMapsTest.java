package com.github.dfauth.functional;

import org.junit.Test;

import java.util.Map;

import static com.github.dfauth.functional.Maps.extendedMap;
import static org.junit.Assert.assertEquals;

public class ExtendedMapsTest {

    public static final Map<String, Integer> REF = Map.of("a", 1, "b", 2, "c", 3);

    @Test
    public void testIt() {
        assertEquals(Map.of("a", 1, "b", 4, "c", 9), extendedMap(REF).map((k, v) -> v*v));
        assertEquals(Map.of("a", 1, "b", 2, "c", 4), extendedMap(REF).merge(Map.of("c",4)));
        assertEquals(Map.of("a", 1, "b", 2, "c", 3), extendedMap(REF).merge(Map.of("c",4), (v1,v2) -> v1));
        assertEquals(Map.of("a", 1, "b", 2, "c", 4), extendedMap(REF).mergeEntry("c",4));
        assertEquals(Map.of("a", 1, "b", 2, "c", 3), extendedMap(REF).mergeEntry("c",4, (v1,v2) -> v1));
    }
}
