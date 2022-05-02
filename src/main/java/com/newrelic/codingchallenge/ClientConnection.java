package com.newrelic.codingchallenge;

import java.util.function.Consumer;

public interface ClientConnection {
    default void onIncomingMessage(Consumer<String> message) {

    }
    default void sendMessage(String message) {

    }
    default  void close() {

    }
}
