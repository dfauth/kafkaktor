package com.github.dfauth.kafka;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class KafkaExecutors {

    public static ExecutorService executor() {
        return executor(100L, TimeUnit.MILLISECONDS);
    }

    public static ExecutorService executor(long keepalive, TimeUnit units) {
        // use the forkjoin pool if parallelism is > 1
        // otherwise construct a custom executor
        // (this is to avoid ForkJoinPool issues observed in underpowered k8 pods
        return Optional.<ExecutorService>of(ForkJoinPool.commonPool())
                .filter(ignored -> ForkJoinPool.getCommonPoolParallelism() > 1)
                .orElse(new ThreadPoolExecutor(keepalive,units));
    }

    public static ThreadFactory threadFactory() {
        return r -> {
            AtomicInteger cnt = new AtomicInteger(0);
            return new Thread(r, KafkaExecutors.class.getSimpleName()+"-"+cnt.getAndIncrement());
        };
    }

    public static class ThreadPoolExecutor extends java.util.concurrent.ThreadPoolExecutor {

        public ThreadPoolExecutor(long keepalive, TimeUnit unit) {
            super(0,
                    Runtime.getRuntime().availableProcessors(),
                    keepalive,
                    unit,
                    new LinkedBlockingQueue<>(),
                    threadFactory(),
                    new ThreadPoolExecutor.AbortPolicy()
            );
        }

    }
}