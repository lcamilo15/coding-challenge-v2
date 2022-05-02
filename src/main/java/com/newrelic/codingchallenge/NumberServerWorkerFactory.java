package com.newrelic.codingchallenge;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.function.Consumer;

public class NumberServerWorkerFactory {
    Consumer<ClientConnection> clientConnectionConsumerList;
    ServerSocket serverSocket;

    public NumberServerWorkerFactory(ServerSocket serverSocket, Consumer<ClientConnection> clientConnectionConsumerList) {
        this.clientConnectionConsumerList = clientConnectionConsumerList;
        this.serverSocket = serverSocket;
    }

    /**
     * Uses the ServerSocket and waits for a connection, once a connection is received it will create an instance
     * of NumberServerWorker.
     * This is leveraging the regular behavior of Java.IO Server Socket
     * @return
     */
    public NumberServerWorker create() throws IOException {
        return new NumberServerWorker(serverSocket.accept(), clientConnectionConsumerList);
    }

}
