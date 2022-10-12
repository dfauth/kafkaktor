package com.github.dfauth.kafka;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class KafkaExecutors {

    private static volatile ThreadPoolExecutor INSTANCE = null;

    private static final Supplier<Executor> configured;

    static {
        configured = ExecutorSelection.fromString(System.getProperty(KafkaExecutors.class.getCanonicalName())).orElse(null);
    }

    public static Executor executor() {
        return executor(100L, TimeUnit.MILLISECONDS);
    }

    public static Executor executor(long keepalive, TimeUnit units) {
        // if configured via system property, use that
        return Optional.ofNullable(configured)
                .map(Supplier::get)
                // otherwise
                .orElseGet(() -> {
                    // use the forkjoin pool if parallelism is > 1
                    // otherwise construct a custom executor
                    // (this is to avoid ForkJoinPool issues observed in underpowered k8 pods)
                    return Optional.<ExecutorService>of(ForkJoinPool.commonPool())
                            .filter(ignored -> ForkJoinPool.getCommonPoolParallelism() > 1)
                            .orElse(getInstance(keepalive,units));
                });
    }

    // shared common pool
    private static ThreadPoolExecutor getInstance(long keepalive, TimeUnit units) {
        if(INSTANCE == null) {
            synchronized (KafkaExecutors.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ThreadPoolExecutor(keepalive, units);
                }
            }
        }
        return INSTANCE;
    }

    public static ThreadFactory threadFactory() {
        return threadFactory(KafkaExecutors.class.getSimpleName());
    }

    public static ThreadFactory threadFactory(String name) {
        AtomicInteger cnt = new AtomicInteger(0);
        return r -> new Thread(r, name+"-"+cnt.getAndIncrement());
    }

    public static class ThreadPoolExecutor extends java.util.concurrent.ThreadPoolExecutor {

        public ThreadPoolExecutor(long keepalive, TimeUnit unit) {
            super(0,
                    Runtime.getRuntime().availableProcessors(),
                    keepalive,
                    unit,
                    new LinkedBlockingQueue<>(),
                    threadFactory(String.format("%s-shared(%d)-",KafkaExecutors.class.getSimpleName(),Runtime.getRuntime().availableProcessors())),
                    new ThreadPoolExecutor.AbortPolicy()
            );
        }

    }

    enum ExecutorSelection implements Supplier<Executor> {

        DEDICATED_SINGLE_THREAD_EXECUTOR(() -> Executors.newSingleThreadExecutor(threadFactory(KafkaExecutors.class.getSimpleName()+"-dedicated"))),
        FORK_JOIN_POOL(() -> ForkJoinPool.commonPool()),
        SHARED_SINGLETON(() -> getInstance(100L, TimeUnit.MILLISECONDS));

        private final Supplier<Executor> supplier;

        ExecutorSelection(Supplier<Executor> supplier) {
            this.supplier = supplier;
        }

        static Optional<ExecutorSelection> fromString(String name) {
            return Optional.ofNullable(name).flatMap(n ->
                Stream.of(values()).filter(v -> v.name().equalsIgnoreCase(n)).findFirst()
            );
        }

        @Override
        public Executor get() {
            return supplier.get();
        }
    }
}