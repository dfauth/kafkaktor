package com.github.dfauth.kafka.cache.subscribable;

import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static com.github.dfauth.kafka.cache.subscribable.CachingPredicate.duplicates;
import static com.github.dfauth.kafka.cache.subscribable.NonCompletingPublisher.supply;
import static org.junit.Assert.assertEquals;

public class CachingPredicateTest {

    @Test
    public void testThis() {
        List<Integer> out = new ArrayList<>();
        Flux.from(supply(1,2,3,1,1,4,4,4,2,1,2,2,5,6)).filter(duplicates()).subscribe(out::add);
        assertEquals(List.of(1,2,3,1,4,2,1,2,5,6), out);
    }

    @Test
    public void testThat() {
    }


}
