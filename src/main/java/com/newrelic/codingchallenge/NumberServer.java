package com.newrelic.codingchallenge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class NumberServer {
    private final int port;
    private final int maxClients;
    private ServerSocket serverSocket;

    Consumer<ClientConnection> clientConnectionConsumerList;

    ExecutorService executorService;

    private List<NumberServerWorker> numberServerWorkerList = Collections.synchronizedList(new ArrayList<>());;
    private Consumer<NumberServer> onShutDownConsumer = (it)->{ };

    Logger logger = LoggerFactory.getLogger(NumberServer.class);

    public NumberServer(int port, int maxClients) {
        this.port = port;
        this.maxClients = maxClients;
        this.executorService = new ThreadPoolExecutor(
                maxClients,
                maxClients,
                0,
                MILLISECONDS,
                new SynchronousQueue<>());
    }

    public void start() {
        try {
            this.serverSocket = new ServerSocket(port);
            NumberServerWorkerFactory numberServerWorkerFactory = new NumberServerWorkerFactory(serverSocket, clientConnectionConsumerList);
            while (!serverSocket.isClosed()) {
                NumberServerWorker numberServerWorker = numberServerWorkerFactory.create();
                try {
                    async(()->{
                        numberServerWorkerList.add(numberServerWorker);
                        numberServerWorker.start();
                        closeGracefully(numberServerWorker);
                    });
                } catch (RejectedExecutionException e) {
                    reject(numberServerWorker);
                }
            }
        }  catch (SocketException e) {
            if (!serverSocket.isClosed()) {
                logger.warn("server SocketException", e);
            }
        } catch (Exception e) {
            logger.error("Unable to process client request. ", e);
        }
    }

    public void reject(NumberServerWorker numberServerWorker) {
        numberServerWorker.close();
    }

    public void shutdown() {
        logger.debug("closing server socket");
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.warn("unable to close serverSocket. ", e);
        }
        executorService.shutdown();
        disconnectAllClients();
        onShutDownConsumer.accept(this);
    }

    synchronized private void disconnectAllClients() {
        for (NumberServerWorker serverWorker : numberServerWorkerList) {
            serverWorker.close();
        }
    }

    public void closeGracefully(NumberServerWorker serverWorker) {
        serverWorker.close();
        numberServerWorkerList.remove(serverWorker);
    }

    private CompletableFuture<Void> async(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executorService);
    }

    public void onConnect(Consumer<ClientConnection> clientConnectionConsumer) {
        clientConnectionConsumerList = clientConnectionConsumer;
    }

    public void onShutDown(Consumer<NumberServer> onShutDownConsumer) {
        this.onShutDownConsumer = onShutDownConsumer;
    }

    public boolean isRunning() {
        return Objects.nonNull(serverSocket) && !serverSocket.isClosed();
    }
}