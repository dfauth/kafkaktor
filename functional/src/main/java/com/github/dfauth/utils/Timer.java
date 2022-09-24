package com.github.dfauth.utils;

import com.github.dfauth.functional.Failure;
import com.github.dfauth.functional.Success;
import com.github.dfauth.functional.Try;
import com.github.dfauth.trycatch.DispatchHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.dfauth.functional.Functions.toCallable;
import static com.github.dfauth.functional.Unit.toFunction;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatch;

@Slf4j
public class Timer {

    private static final ThreadLocal<Timer> tLocal = ThreadLocal.withInitial(Timer::new);
    private static final ThreadLocal<Receiver> tLocalReceiver = ThreadLocal.withInitial(Receiver::new);

    private final Stack<Split> stack = new Stack<>();
    private final List<Split> splits = new ArrayList<>();

    public static void register(Consumer<List<Split>> c) {
        tLocalReceiver.get().register(c);
    }

    public static void withTimer(String key, Runnable runnable) {
        tryCatch(() -> withTimer(key, toCallable(runnable)));
    }

    public static <T> T withTimer(String key, Callable<T> callable) throws Exception {
        return withTimer1(key, ignored -> Try.trySilentlyWithCallable(callable));
    }

    public static void withTimer(String key, Consumer<TimerControl> c) {
        withTimer(key, (TimerControl control) -> toFunction(c).apply(control));
    }

    public static <T> T withTimer(String key, Function<TimerControl, T> f) {
        try {
            return withTimer1(key, control -> Try.trySilentlyWithCallable(() -> f.apply(control)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T withTimer1(String key, Function<TimerControl, Try<T>> f) throws Exception {
        Timer timer = tLocal.get();
        try {
            return f.apply(timer.mark(key)).dispatch(new DispatchHandler<>() {
                @Override
                public T dispatch(Failure<T> f) {
                    throw new RuntimeException(f.exception());
                }

                @Override
                public T dispatch(Success<T> s) {
                    return s.result();
                }
            });
        } catch (RuntimeException e) {
            if(e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            } else {
                throw e;
            }
        } finally {
            timer.elapsed();
        }
    }

    private Split mark(String key) {
        Split split = new Split(key);
        stack.push(split);
        splits.add(split);
        return split;
    }

    private void elapsed() {
        stack.pop().split(stack.size());
        if(stack.isEmpty()) {
            tLocal.remove();
            tLocalReceiver.get().callback(splits);
//            log.info(render(splits));
        }
    }

    public static String render(List<Split> splits) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        List<String> strings = splits.stream().filter(s -> !s.isIgnorable()).map(Split::toString).collect(Collectors.toList());
        PrintWriter pw = new PrintWriter(stream);
        pw.println("timings:");
        strings.forEach(pw::println);
        pw.close();
        return stream.toString();
    }

    public interface TimerControl {
        void ignore();
    }

    public static class Split implements TimerControl {

        private SplitState state;

        public Split(String key) {
            this.state = new Started(key);
        }

        @Override
        public String toString() {
            return this.state.toString();
        }

        public void split(int depth) {
            state = state.split(depth);
        }

        @Override
        public void ignore() {
            this.state = state.ignore();
        }

        public boolean isIgnorable() {
            return state.isIgnorable();
        }

        public String getKey() {
            return state.key;
        }

        public SplitState getState() {
            return state;
        }
    }

    public static abstract class SplitState {

        protected final String key;

        public SplitState(String key) {
            this.key = key;
        }

        public abstract SplitState split(int depth);

        public abstract SplitState ignore();

        public boolean isIgnorable() {
            return false;
        }
    }

    private static class Started extends SplitState {
        private final long startTime = System.currentTimeMillis();

        public Started(String key) {
            super(key);
        }

        @Override
        public SplitState split(int depth) {
            return new Finished(key, startTime, depth);
        }

        @Override
        public SplitState ignore() {
            return new Ignored(key);
        }
    }

    public static class Finished extends SplitState {

        private final int depth;
        private final long elapsed;

        public Finished(String key, long startTime, int depth) {
            super(key);
            this.depth = depth;
            this.elapsed = System.currentTimeMillis() - startTime;
        }

        @Override
        public SplitState split(int depth) {
            return this;
        }

        @Override
        public String toString() {
            String margin = IntStream.range(0, depth).mapToObj(j -> "  ").collect(Collectors.joining());
            return String.format("%s%s: %d msec", margin, key, elapsed);
        }

        @Override
        public SplitState ignore() {
            return new Ignored(key);
        }

        public long getElapsed() {
            return elapsed;
        }

        public int getDepth() {
            return depth;
        }
    }

    private static class Ignored extends SplitState {

        public Ignored(String key) {
            super(key);
        }

        @Override
        public SplitState split(int depth) {
            return this;
        }

        public boolean isIgnorable() {
            return true;
        }

        public SplitState ignore() {
            return this;
        }
    }

    public static class Receiver {
        private Consumer<List<Split>> callback = splits -> log.info(render(splits));

        void register(Consumer<List<Split>> c) {
            this.callback = c;
        }

        void callback(List<Split> splits) {
            this.callback.accept(splits);
        }
    }
}
