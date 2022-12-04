package com.github.dfauth.kafkaktor;

import com.github.dfauth.avro.MessageContext;
import org.apache.avro.specific.SpecificRecord;

import java.util.Map;

public interface AktorMessageContext extends MessageContext {

    String SENDER_KEY = "senderKey";

    String key();

    Map<String, Object> metadata();

    <R extends SpecificRecord> AktorReference<R> sender();
}
