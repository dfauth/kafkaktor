package com.github.dfauth.functional;

import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.dfauth.functional.Functions.peek;
import static com.github.dfauth.functional.Functions.toPredicate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FunctionsTestCase {

    private static final Function<String, Try<Integer>> REF1 = t -> Try.tryWithCallable(() -> Integer.valueOf(t));
    private static final Function<String, Boolean> REF2 = REF1.andThen(Try::isSuccess);
    private static final Function<AtomicReference<Try<Integer>>, Consumer<String>> REF3 = r -> t -> r.set(REF1.apply(t));

    @Test
    public void testIt() {
        String t = "1";
        assertEquals(1, Stream.of(t).filter(toPredicate(REF2)).count());
        AtomicReference<Try<Integer>> x = new AtomicReference<>();
        Consumer<String> b = REF3.apply(x);
        assertEquals(Collections.singletonList("1"), Stream.of(t).map(peek(b)).collect(Collectors.toList()));
        assertTrue(x.get().isSuccess());
        assertEquals(1, (int)x.get().toSuccess().result());
    }

}
