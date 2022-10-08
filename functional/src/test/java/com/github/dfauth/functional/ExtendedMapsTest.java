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
    }
}
