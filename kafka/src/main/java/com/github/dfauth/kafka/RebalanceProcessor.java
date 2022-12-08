package com.github.dfauth.kafka;

import com.github.dfauth.functional.Maps;
import com.github.dfauth.kafka.recovery.PartitionRecoveryListener;
import com.github.dfauth.kafka.recovery.RecoveryState;
import com.github.dfauth.trycatch.TryCatch;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static com.github.dfauth.kafka.recovery.RecoveryState.initial;

public interface RebalanceProcessor<K,V,T> extends KafkaConsumerAware<K,V,Function<Collection<TopicPartition>,T>>{

    static <K,V> RebalanceProcessor<K,V,Map<TopicPartition, RecoveryState>> currentOffsets(PartitionRecoveryListener recoveryListener) {
        return c -> tps -> Maps.generate(tps, tp -> initial(tp, c.position(tp)-1, recoveryListener));
    }

    default RebalanceProcessor<K,V,T> andThen(RebalanceListener<K,V> nested) {
        return c -> tps ->
            TryCatch.CallableBuilder.tryCatch((Callable<T>) () -> withKafkaConsumer(c).apply(tps))
                    .andFinallyRun(() -> nested.withKafkaConsumer(c).accept(tps));
    }
}
