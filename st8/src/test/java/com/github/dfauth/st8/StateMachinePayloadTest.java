package com.github.dfauth.st8;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.github.dfauth.st8.TestEvent.*;
import static com.github.dfauth.st8.TestState.*;
import static org.junit.Assert.*;

@Slf4j
public class StateMachinePayloadTest {

    private boolean[] bool = new boolean[] {true};
    private Queue<String> q = new ArrayDeque<>();

    @Test
    public void testIt() {
        try {
            /**
             *        A
             *      /  \
             *    A1   A2
             *   /      \
             *  B---B1---C
             *   \      /
             *    B2  C1
             *     \ /
             *      D
             */
            StateMachine.Builder<TestState, StateMachinePayloadTest, TestEvent, List<String>,String> builder = StateMachine.create("name", this);
            assertNotNull(builder);

            builder.initial(A)
                    .onEntry(queuingConsumer("onEntry(A)"))
                    .onExit(queuingConsumer("onExit(A)"))
                    .onEvent(A1)
                    .unless(predicate())
                    .onTransition(testTransitionListener())
                    .goTo(B)
                    .onEvent(A2)
                    .unless(predicate())
                    .goTo(C)
                    .onTransition(testTransitionListener())
                    .state(B)
                    .onEntry(queuingConsumer("onEntry(B)"))
                    .onExit(queuingConsumer("onExit(B)"))
                    .onEvent(B1)
                    .unless(predicate())
                    .onTransition(testTransitionListener())
                    .goTo(C)
                    .onEvent(B2)
                    .unless(predicate())
                    .goTo(D)
                    .onTransition(testTransitionListener())
                    .state(C)
                    .onEntry(queuingConsumer("onEntry(C)"))
                    .onExit(queuingConsumer("onExit(C)"))
                    .onEvent(C1)
                    .unless(predicate())
                    .onTransition(testTransitionListener())
                    .goTo(D)
                    .state(D)
                    .onEntry(queuingConsumer("onEntry(D)"))
                    .onExit(queuingConsumer("onExit(D)"))
            ;

            {
                StateMachine<TestState, StateMachinePayloadTest, TestEvent, List<String>, String> stateMachine = builder.build();
                assertEquals(A, stateMachine.currentState().type);
                assertTrue(stateMachine.currentState().payload().isEmpty());
            }

            {
                StateMachine<TestState, StateMachinePayloadTest, TestEvent, List<String>, String> stateMachine = builder.build();
                assertEquals(B, stateMachine.onEvent(asEvent(A1), this).currentState().type);
                assertEquals("onExit(A)", q.poll());
                assertEquals(List.of("A->A1->B"), stateMachine.currentState().payload().get());
                assertEquals("onEntry(B)", q.poll());
            }

            {
                StateMachine<TestState, StateMachinePayloadTest, TestEvent, List<String>, String> stateMachine = builder.build();
                assertEquals(B, stateMachine.onEvent(asEvent(A1), this).currentState().type);
                assertEquals("onExit(A)", q.poll());
                assertEquals(List.of("A->A1->B"), stateMachine.currentState().payload().get());
                assertEquals("onEntry(B)", q.poll());
                assertEquals(C, stateMachine.onEvent(asEvent(B1), this).currentState().type);
                assertEquals("onExit(B)", q.poll());
                assertEquals(List.of("A->A1->B","B->B1->C"), stateMachine.currentState().payload().get());
                assertEquals("onEntry(C)", q.poll());
                assertEquals(D, stateMachine.onEvent(asEvent(C1), this).currentState().type);
                assertEquals("onExit(C)", q.poll());
                assertEquals(List.of("A->A1->B","B->B1->C","C->C1->D"), stateMachine.currentState().payload().get());
                assertEquals("onEntry(D)", q.poll());
            }

            {
                StateMachine<TestState, StateMachinePayloadTest, TestEvent, List<String>, String> stateMachine = builder.build();
                assertEquals(B, stateMachine.onEvent(asEvent(A1), this).currentState().type);
                assertEquals("onExit(A)", q.poll());
                assertEquals(List.of("A->A1->B"), stateMachine.currentState().payload().get());
                assertEquals("onEntry(B)", q.poll());
                assertEquals(D, stateMachine.onEvent(asEvent(B2), this).currentState().type);
                assertEquals("onExit(B)", q.poll());
                assertEquals(List.of("A->A1->B","B->B2->D"), stateMachine.currentState().payload().get());
                assertEquals("onEntry(D)", q.poll());
            }
            {
                StateMachine<TestState, StateMachinePayloadTest, TestEvent, List<String>, String> stateMachine = builder.build();
                assertEquals(C, stateMachine.onEvent(asEvent(A2), this).currentState().type);
                assertEquals("onExit(A)", q.poll());
                assertEquals(List.of("A->A2->C"), stateMachine.currentState().payload().get());
                assertEquals("onEntry(C)", q.poll());
                assertEquals(D, stateMachine.onEvent(asEvent(C1), this).currentState().type);
                assertEquals("onExit(C)", q.poll());
                assertEquals(List.of("A->A2->C","C->C1->D"), stateMachine.currentState().payload().get());
                assertEquals("onEntry(D)", q.poll());
            }

            {
                bool[0] = false;
                StateMachine<TestState, StateMachinePayloadTest, TestEvent, List<String>, String> stateMachine = builder.build();
                assertEquals(A, stateMachine.onEvent(asEvent(A1), this).currentState().type);
                assertTrue(q.isEmpty());
                assertEquals(A, stateMachine.onEvent(asEvent(B1), this).currentState().type);
                assertTrue(q.isEmpty());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private <E> Event<E,String> asEvent(E e) {
        return new Event<E, String>() {
            @Override
            public E type() {
                return e;
            }

            @Override
            public Optional<String> payload() {
                return Optional.ofNullable(type().toString());
            }
        };
    }

    private Consumer<StateMachinePayloadTest> loggingConsumer(String message) {
        return t -> log.info(message+": "+t);
    }

    private Consumer<StateMachinePayloadTest> queuingConsumer(String message) {
        return t -> q.offer(message);
    }

    private <T,U,V> TransitionListener<T,U,V,List<String>,String> testTransitionListener() {
        return t -> u -> v -> r -> {
                List<String> tmp = t.payload().map(ArrayList::new).orElse(new ArrayList<>());
                u.payload().ifPresent(_u -> tmp.add(t.type +"->"+_u+"->"+r));
                return Optional.of(tmp);
            };
    }

    private Predicate<StateMachinePayloadTest> predicate() {
        return u -> u.bool[0];
    }

}