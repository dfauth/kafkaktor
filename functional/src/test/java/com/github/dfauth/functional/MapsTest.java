package com.github.dfauth.functional;

import org.junit.Test;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class MapsTest {

    private static final Map<String, Integer> m1 = Map.of("a", 1, "b", 2, "c", 3);
    private static final Map<String, Integer> m2 = Map.of("d", 4, "e", 5, "f", 6);
    private static final Map<String, Integer> m3 = Map.of("g", 7, "h", 8, "i", 9);
    private static final Map<String, Integer> m4 = Map.of("a", 10, "b", 11, "c", 12);

    @Test
    public void testIt() {

        {
            Map<String, Integer> result = Maps.concat(m1, m2, m3);
            _assertEquals(result,m1,"a","b","c");
            _assertEquals(result,m2,"d","e","f");
            _assertEquals(result,m3,"g","h","i");
        }

        {
            Map<String, Integer> result = Maps.concat(m1, m2, m3, m4);
            _assertEquals(result,m4,"a","b","c");
            _assertEquals(result,m2,"d","e","f");
            _assertEquals(result,m3,"g","h","i");
        }

        {
            Map<String, Integer> result = Maps.concat((v1,v2) -> v1, m1, m2, m3, m4);
            _assertEquals(result,m1,"a","b","c");
            _assertEquals(result,m2,"d","e","f");
            _assertEquals(result,m3,"g","h","i");
        }

    }

    @Test
    public void testMap() {
        assertEquals(Map.of("a","1","b","2","c","3"), Maps.map(m1, String::valueOf));
    }

    private void _assertEquals(Map<String, Integer> result, Map<String, Integer> m, String... keys) {
        Stream.of(keys).forEach(k -> {
            assertEquals((int)result.get(k), (int)m.get(k));
        });
    }
}
