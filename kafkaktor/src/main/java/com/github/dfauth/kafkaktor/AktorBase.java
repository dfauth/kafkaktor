package com.github.dfauth.kafkaktor;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;

@Slf4j
public abstract class AktorBase<T extends SpecificRecord> implements Aktor<T> {

    protected final AktorContext ctx;

    protected AktorBase(AktorContext ctx) {
        this.ctx = ctx;
    }

}
