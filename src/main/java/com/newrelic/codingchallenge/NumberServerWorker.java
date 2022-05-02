package com.newrelic.codingchallenge;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Represents the worker handling requests from inbound/outbound messages
 * This is meant to be wrapped by on a Thread
 */
public class NumberServerWorker {
    Socket listenerSocket;

    private Consumer<ClientConnection> clientConnection;

    boolean running = false;

    public NumberServerWorker(Socket listenerSocket,  Consumer<ClientConnection> clientConnection) {
        this.listenerSocket = listenerSocket;
        this.clientConnection = clientConnection;
    }

    public void start() {
        running = true;
        try (
                InputStreamReader inputStreamReader = new InputStreamReader(listenerSocket.getInputStream(), StandardCharsets.UTF_8);
                BufferedReader inputReader = new BufferedReader(inputStreamReader);
                OutputStream outputFromServer = listenerSocket.getOutputStream();
                PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputFromServer, StandardCharsets.UTF_8), true);
        ){
            AtomicBoolean breakLoop = new AtomicBoolean(false);
            clientConnection.accept(new ClientConnection() {
                @Override
                public void onIncomingMessage(Consumer<String> message) {
                    String inputLine;
                    while (true) {
                        try {
                            if (!(running && (inputLine = inputReader.readLine()) != null)) break;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        message.accept(inputLine);
                        if (breakLoop.get()) {
                            break;
                        }
                    }
                }
                @Override
                public void sendMessage(String message) {
                    printWriter.println(message);
                }
                @Override
                public void close() {
                    breakLoop.set(true);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        running = false;
        if (!listenerSocket.isClosed()) {
            try {
                listenerSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
