package com.github.dfauth.trycatch;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.spi.LoggerFactoryBinder;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssertingLogger implements LoggerFactoryBinder, ILoggerFactory {

    private static final Queue<Call> q = new ArrayDeque<>();

    public static Queue<Call> getQueue() {
        return q;
    }

    public static <T> Logger of(Logger delegate) {
        return (Logger) Proxy.newProxyInstance(AssertingLogger.class.getClassLoader(), new Class[]{Logger.class}, (proxy, method, args) -> {
            q.offer(Call.of(method.getName(), args));
            method.invoke(delegate, args);
            return null;
        });
    }

    public static void assertNothingLogged() {
        assertTrue("Expected no logging message, but found: "+q.peek(),q.isEmpty());
    }

    public static void resetLogEvents() {
        q.clear();
    }

    public static <T> T withCleanup(Callable<T> callable) {
        return tryCatch(() -> {
            resetLogEvents();
            return callable.call();
        }, AssertingLogger::resetLogEvents);
    }

    public static void withCleanup(Runnable runnable) {
        withCleanup((Callable<Void>) () -> {
            runnable.run();
            return null;
        });
    }

    public static void assertExceptionLogged(Throwable t) {
        assertFalse(String.format("Expected exception %s, but found nothing was logged",t.getMessage()),q.isEmpty());
        Call call = q.remove();
        assertTrue(call.isError());
        assertTrue("Expected "+t.getMessage()+" but received "+call.argument(0).get(), call.argument(0).map(_t -> _t.equals(t.getMessage())).orElse(false));
        assertTrue(call.argument(1).map(_t -> _t.getClass() == t.getClass()).orElse(false));
    }

    public static void assertInfoLogged(String message) {
        assertInfoLogged(msg -> msg.equals(message));
    }

    public static void assertInfoLogged(Predicate<String>p) {
        assertFalse(q.isEmpty());
        Call call = q.remove();
        assertTrue(call.isInfo());
        assertTrue(call.argument(0).map(_t -> p.test((String)_t)).orElse(false));
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return this;
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return this.getClass().getName();
    }

    @Override
    public Logger getLogger(String name) {
        return (Logger) Proxy.newProxyInstance(AssertingLogger.class.getClassLoader(), new Class[]{Logger.class}, (proxy, method, args) -> {
            q.offer(Call.of(method.getName(), args));
            return null;
        });
    }

    public static class Call {

        private final String methodName;
        private final Object[] args;

        public Call(String methodName, Object[] args) {

            this.methodName = methodName;
            this.args = args;
        }

        static Call of(String methodName, Object[] args) {
            return new Call(methodName, args);
        }

        public boolean isError() {
            return "error".equals(methodName);
        }

        public Optional<Object> argument(int i) {
            return args.length <= i ? Optional.empty() : Optional.ofNullable(args[i]);
        }

        public boolean isInfo() {
            return "info".equals(methodName);
        }

        @Override
        public String toString() {
            return String.format("call to method %s with arguments %s", methodName, Arrays.asList(args).stream().map(Object::toString).collect(Collectors.joining(",")));
        }
    }
}
