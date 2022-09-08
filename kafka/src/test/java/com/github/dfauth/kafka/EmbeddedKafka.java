package com.github.dfauth.kafka;

import com.github.dfauth.kafka.assertion.AsynchronousAssertions;
import com.github.dfauth.kafka.assertion.AsynchronousAssertionsAware;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.dfauth.kafka.CompletableFutureAware.runProvidingFuture;
import static com.github.dfauth.kafka.assertion.AsynchronousAssertionsAware.runProvidingAsynchronousAssertions;
import static com.github.dfauth.trycatch.TryCatch._Callable.tryCatch;
import static com.github.dfauth.trycatch.TryCatch._Runnable.tryCatchIgnore;

public class EmbeddedKafka {

    public static EmbeddedKafkaRunner embeddedKafkaWithTopics(String... topics) {
        return withEmbeddedKafka(topics, Collections.emptyMap());
    }

    public static EmbeddedKafkaRunner withEmbeddedKafka() {
        return withEmbeddedKafka(Collections.emptyMap());
    }

    public static EmbeddedKafkaRunner withEmbeddedKafka(Map<String, String> brokerConfig) {
        Map<String, String> tmp = new HashMap<>(brokerConfig);
        tmp.merge("auto.create.topics.enable","true", (k,v)-> v);
        return new EmbeddedKafkaRunner(new String[0], tmp);
    }

    public static EmbeddedKafkaRunner withEmbeddedKafka(String[] topics, Map<String, String> config) {
        return new EmbeddedKafkaRunner(topics, config);
    }

    private static void terminate(EmbeddedKafkaBroker broker) {
        tryCatchIgnore(() ->
                broker.destroy()
        );
    }

    public static class EmbeddedKafkaRunner {

        private final String[] topics;
        private final Map<String, String> brokerConfig;
        private Map<String, Object> clientConfig = new HashMap();
        private int partitions;

        public EmbeddedKafkaRunner(String[] topics, Map<String, String> brokerConfig) {
            this(topics, brokerConfig, 1);
        }

        public EmbeddedKafkaRunner(String[] topics, Map<String, String> brokerConfig, int partitions) {
            this.topics = topics;
            this.brokerConfig = brokerConfig;
            this.partitions = partitions;
        }

        public EmbeddedKafkaRunner withGroupId(String groupId) {
            Map<String, Object> tmp = new HashMap(clientConfig);
            tmp.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            this.clientConfig = tmp;
            return this;
        }

        public EmbeddedKafkaRunner withPartitions(int partitions) {
            this.partitions = partitions;
            return this;
        }

        public void runTest(Consumer<Map<String, Object>> consumer) {
            runTestFuture(p -> tryCatch(() -> {
                consumer.accept(p);
                return CompletableFuture.completedFuture(null);
            }));
        }

        public <T> T runTest(Function<Map<String, Object>, T> f) {
            EmbeddedKafkaBroker broker = new EmbeddedKafkaBroker(1, true, partitions, topics);
            broker.brokerProperties(brokerConfig);
            broker.afterPropertiesSet();
            Map<String, Object> p = new HashMap(this.clientConfig);
            p.putAll(ImmutableMap.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()));
            try {
                return f.apply(p);
            } finally {
                terminate(broker);
            }
        }

        public <T> CompletableFuture<T> runTestFuture(Function<Map<String, Object>, CompletableFuture<T>> f) {
            EmbeddedKafkaBroker broker = new EmbeddedKafkaBroker(1, true, partitions, topics);
            broker.afterPropertiesSet();
            Map<String, Object> p = new HashMap(this.clientConfig);
            p.putAll(ImmutableMap.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()));
            CompletableFuture<T> _f = f.apply(p);
            return _f.handle((r,e) -> {
                terminate(broker);
                return r;
            });
        }

        public <T> CompletableFuture<T> runAsyncTest(CompletableFutureAware<T,Map<String, Object>> aware) {
            EmbeddedKafkaBroker broker = new EmbeddedKafkaBroker(1, true, partitions, topics);
            broker.afterPropertiesSet();
            Map<String, Object> p = new HashMap(this.clientConfig);
            p.putAll(ImmutableMap.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()));
            return runProvidingFuture(aware).apply(p).handle((r,e) -> {
                terminate(broker);
                return r;
            });
        }

        public AsynchronousAssertions runWithAssertions(AsynchronousAssertionsAware<Map<String, Object>> aware) {
            EmbeddedKafkaBroker broker = new EmbeddedKafkaBroker(1, true, partitions, topics);
            broker.afterPropertiesSet();
            Map<String, Object> p = new HashMap(this.clientConfig);
            p.putAll(ImmutableMap.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()));
            AsynchronousAssertions assertions = runProvidingAsynchronousAssertions(aware).apply(p);
            assertions.future().handle((r,e) -> {
                terminate(broker);
                return r;
            });
            return assertions;
        }
    }
}
