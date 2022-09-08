package com.github.dfauth.kafkaktor.st8;

public enum KafkaConsumerState {
    INITIAL(), ASSIGNED(true), REVOKED();

    private final boolean connected;

    KafkaConsumerState() {
        this(false);
    }

    KafkaConsumerState(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }
}
