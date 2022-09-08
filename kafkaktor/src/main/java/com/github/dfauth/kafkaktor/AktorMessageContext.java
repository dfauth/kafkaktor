package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.MessageContext;

import java.util.Map;

public interface AktorMessageContext extends MessageContext {

    String SENDER_TOPIC = "senderTopic";
    String SENDER_KEY = "senderKey";
    String SENDER_PARTITION = "senderPartition";

    String key();

    Map<String, Object> metadata();

    <R> AktorReference<R> sender();
}
