package com.github.dfauth.st8;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.github.dfauth.st8.TestEvent.*;
import static com.github.dfauth.st8.TestState.*;
import static com.github.dfauth.st8.TransitionListener.sourceAndDestinationStates;
import static org.junit.Assert.*;

@Slf4j
public class StateMachineTest {

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
            StateMachine.Builder<TestState, StateMachineTest, TestEvent, List<String>, List<String>> builder = StateMachine.create("name", this);
            assertNotNull(builder);

            builder.initial(A)
                    .onEntry(queuingConsumer("onEntry(A)"))
                    .onExit(queuingConsumer("onExit(A)"))
                    .onEvent(A1)
                    .unless(predicate())
                    .onTransition(queuingTriConsumer("onTransition(A1)"))
                    .goTo(B)
                    .onEvent(A2)
                    .unless(predicate())
                    .goTo(C)
                    .onTransition(queuingTriConsumer("onTransition(A2)"))
                    .state(B)
                    .onEntry(queuingConsumer("onEntry(B)"))
                    .onExit(queuingConsumer("onExit(B)"))
                    .onEvent(B1)
                    .unless(predicate())
                    .onTransition(queuingTriConsumer("onTransition(B1)"))
                    .goTo(C)
                    .onEvent(B2)
                    .unless(predicate())
                    .goTo(D)
                    .onTransition(queuingTriConsumer("onTransition(B2)"))
                    .state(C)
                    .onEntry(queuingConsumer("onEntry(C)"))
                    .onExit(queuingConsumer("onExit(C)"))
                    .onEvent(C1)
                    .unless(predicate())
                    .onTransition(queuingTriConsumer("onTransition(C1)"))
                    .goTo(D)
                    .state(D)
                    .onEntry(queuingConsumer("onEntry(D)"))
                    .onExit(queuingConsumer("onExit(D)"))
            ;

            {
                StateMachine stateMachine = builder.build();
                assertEquals(A, stateMachine.currentState().type);
            }

            {
                StateMachine stateMachine = builder.build();
                assertEquals(B, stateMachine.onEvent(asEvent(A1), this).currentState().type);
                assertEquals("onExit(A)", q.poll());
                assertEquals("onTransition(A1)", q.poll());
                assertEquals("onEntry(B)", q.poll());
            }

            {
                StateMachine<TestState, StateMachineTest, TestEvent,List<String>, List<String>> stateMachine = builder.build();
                assertEquals(B, stateMachine.onEvent(asEvent(A1), this).currentState().type);
                assertEquals("onExit(A)", q.poll());
                assertEquals("onTransition(A1)", q.poll());
                assertEquals("onEntry(B)", q.poll());
                assertEquals(C, stateMachine.onEvent(asEvent(B1), this).currentState().type);
                assertEquals("onExit(B)", q.poll());
                assertEquals("onTransition(B1)", q.poll());
                assertEquals("onEntry(C)", q.poll());
                assertEquals(D, stateMachine.onEvent(asEvent(C1), this).currentState().type);
                assertEquals("onExit(C)", q.poll());
                assertEquals("onTransition(C1)", q.poll());
                assertEquals("onEntry(D)", q.poll());
            }
            {
                StateMachine<TestState, StateMachineTest, TestEvent,List<String>, List<String>> stateMachine = builder.build();
                assertEquals(B, stateMachine.onEvent(asEvent(A1), this).currentState().type);
                assertEquals("onExit(A)", q.poll());
                assertEquals("onTransition(A1)", q.poll());
                assertEquals("onEntry(B)", q.poll());
                assertEquals(D, stateMachine.onEvent(asEvent(B2), this).currentState().type);
                assertEquals("onExit(B)", q.poll());
                assertEquals("onTransition(B2)", q.poll());
                assertEquals("onEntry(D)", q.poll());
            }
            {
                StateMachine<TestState, StateMachineTest, TestEvent,List<String>, List<String>> stateMachine = builder.build();
                assertEquals(C, stateMachine.onEvent(asEvent(A2), this).currentState().type);
                assertEquals("onExit(A)", q.poll());
                assertEquals("onTransition(A2)", q.poll());
                assertEquals("onEntry(C)", q.poll());
                assertEquals(D, stateMachine.onEvent(asEvent(C1), this).currentState().type);
                assertEquals("onExit(C)", q.poll());
                assertEquals("onTransition(C1)", q.poll());
                assertEquals("onEntry(D)", q.poll());
            }

            {
                bool[0] = false;
                StateMachine<TestState, StateMachineTest, TestEvent,List<String>, List<String>> stateMachine = builder.build();
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

    private <E,X> Event<E,X> asEvent(E e) {
        return () -> e;
    }

    private Consumer<StateMachineTest> loggingConsumer(String message) {
        return t -> log.info(message+": "+t);
    }

    private Consumer<StateMachineTest> queuingConsumer(String message) {
        return t -> q.offer(message);
    }

    private <T,U,V,W,X> TransitionListener<T,U,V,W,X> queuingTriConsumer(String message) {
        return sourceAndDestinationStates((t, r) -> {
            q.offer(message);
            return t.payload;
        });
    }

    private Predicate<StateMachineTest> predicate() {
        return u -> u.bool[0];
    }

}