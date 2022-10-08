package com.github.dfauth.functional;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.github.dfauth.functional.Reducer.groupingMappingReducer;
import static com.github.dfauth.functional.Reducer.mapEntryGroupingMappingReducer;
import static org.junit.Assert.assertEquals;

public class ReducerTest {

    public static final List<Integer> REF = List.of(1,2,3,4,5,6,7,8);
    public static final List<Map.Entry<String, Integer>> REF1 = List.of(
            Tuple2.of("prime", 11).toMapEntry(),
            Tuple2.of("nonprime", 12).toMapEntry(),
            Tuple2.of("prime", 13).toMapEntry(),
            Tuple2.of("nonprime", 14).toMapEntry(),
            Tuple2.of("nonprime", 15).toMapEntry(),
            Tuple2.of("nonprime", 16).toMapEntry(),
            Tuple2.of("prime", 17).toMapEntry(),
            Tuple2.of("nonprime", 18).toMapEntry()
    );
    public static final Function<Integer, String> classifier = i -> i%2 == 0 ? "EVEN" : "ODD";
    public static final Function<Integer, String> valueMapper = String::valueOf;

    @Test
    public void testIt() {
        Reducer<Integer, Map<String, List<String>>> r = groupingMappingReducer(classifier, valueMapper);
        assertEquals(r.reduce(REF), Map.of("EVEN", List.of("2","4","6","8"),"ODD", List.of("1","3","5","7")));
    }

    @Test
    public void testMapEntryGroupingMappingReducer() {
        Reducer<Map.Entry<String, Integer>, Map<String, List<Integer>>> r = mapEntryGroupingMappingReducer();
        assertEquals(r.reduce(REF1), Map.of("prime", List.of(11,13,17),"nonprime", List.of(12,14,15,16,18)));
    }
}
