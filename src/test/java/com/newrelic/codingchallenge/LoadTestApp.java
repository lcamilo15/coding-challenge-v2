package com.newrelic.codingchallenge;

import com.google.common.collect.Lists;
import com.newrelic.codingchallenge.service.NumberBufferedFileLogger;
import com.newrelic.codingchallenge.service.NumberTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.newrelic.codingchallenge.TestUtils.*;

public class LoadTestApp {
    Integer freePort = findFreePort();
    Integer maxClients = 5;
    int checkNumberTrackInSeconds = 10;
    ThreadPoolExecutor executor;
    NumberServer numberServer;
    NumberServerApp numberServerApp;
    File tempLoggingFile;
    NumberTracker numberTracker;
    private NumberBufferedFileLogger numberBufferedFileLogger
            ;

    @BeforeEach
    public void initServer(@TempDir Path tempDir) {
        tempLoggingFile = tempDir.resolve("numbers.log").toFile();
        numberServer = new NumberServer(freePort, maxClients);
        numberBufferedFileLogger = new NumberBufferedFileLogger(tempLoggingFile);
        numberTracker = new NumberTracker(numberBufferedFileLogger);
        numberServerApp = new NumberServerApp(tempLoggingFile, freePort, checkNumberTrackInSeconds, maxClients, numberServer, numberTracker);
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1000);
        executor.execute(numberServerApp::startServer);
    }

    @Test
    public void generate_unique_numbers() throws Exception {
        int maxMessages = 2 * 1000000;
        int periodInSeconds = 10;
        List<String> generatedMessages = generateUniqueMessages(maxMessages);
        MemoryUtils.MemorySnapShot memorySnapShot = runTest(generatedMessages, periodInSeconds);

        int indexedNumbers = numberTracker.getIndexedNumbers().size();
        int numbersSavedToFile = Files.readAllLines(tempLoggingFile.toPath()).size();

        long startMemUsage = memorySnapShot.startMemUsage;
        long endMemoryUse = memorySnapShot.endMemUsage;
        long memoryUsedDuringCalls = endMemoryUse- startMemUsage;
        long timeElapsed = memorySnapShot.getTimeElapsed();

        System.out.println("***** Mem utilization statistics [MB] *****");
        System.out.printf("Start Memory Used:  %dMB %n", startMemUsage);
        System.out.printf("End Used: %dMB %n", endMemoryUse);
        System.out.printf("Diff: %dMB %n", memoryUsedDuringCalls);

        System.out.printf("Numbers tracked %d %n", indexedNumbers);
        System.out.printf("Numbers saved to file %d %n", numbersSavedToFile);

        System.out.printf("Took %d seconds to run %n", timeElapsed);
    }

    @Test
    public void generate_random_numbers() throws Exception {
        int maxMessages = 2 * 1000000;
        int periodInSeconds = 10;
        List<String> generatedMessages = generateRandomMessages(maxMessages, 0, 999999999);
        MemoryUtils.MemorySnapShot memorySnapShot = runTest(generatedMessages, periodInSeconds);

        int indexedNumbers = numberTracker.getIndexedNumbers().size();
        int numbersSavedToFile = Files.readAllLines(tempLoggingFile.toPath()).size();

        long startMemUsage = memorySnapShot.startMemUsage;
        long endMemoryUse = memorySnapShot.endMemUsage;
        long memoryUsedDuringCalls = endMemoryUse- startMemUsage;
        long timeElapsed = memorySnapShot.getTimeElapsed();

        System.out.println("***** Mem utilization statistics [MB] *****");
        System.out.printf("Start Memory Used:  %dMB %n", startMemUsage);
        System.out.printf("End Used: %dMB %n", endMemoryUse);
        System.out.printf("Diff: %dMB %n", memoryUsedDuringCalls);

        System.out.printf("Numbers tracked %d %n", indexedNumbers);
        System.out.printf("Numbers saved to file %d %n", numbersSavedToFile);

        System.out.printf("Took %d seconds to run %n", timeElapsed);
    }


    public MemoryUtils.MemorySnapShot runTest(List<String> messagesToSend, int periodInSeconds) throws InterruptedException, IOException {
        int maxMessages = messagesToSend.size();
        int messagesPerClient = maxMessages / maxClients;
        int messagesPerClientPerInterval = messagesPerClient / periodInSeconds;

        //TrackedMemory Right before executing threads
        MemoryUtils.MemorySnapShot memorySnapShot = new MemoryUtils.MemorySnapShot();
        Set<String> messagesSent = ConcurrentHashMap.newKeySet();
        CountDownLatch messagesSentCountDownLatch = new CountDownLatch(maxClients);
        ExecutorService executorService = Executors.newFixedThreadPool(maxClients);
        Lists.partition(messagesToSend, messagesPerClient).forEach((generatedMessagesForClient) -> {
            executorService.execute(() -> {
                for (List<String> messages : Lists.partition(generatedMessagesForClient, messagesPerClientPerInterval)) {
                    sendMessagesToSever(freePort, messages, 200);
                    messagesSent.addAll(messages);
                }
                messagesSentCountDownLatch.countDown();
            });
        });

        messagesSentCountDownLatch.await();
        Thread.sleep(periodInSeconds * 1000);

        numberServer.shutdown();
        memorySnapShot.captureMemUsage();

        return memorySnapShot;
    }

    private List<String> generateRandomMessages(int numberOfMessages, int lowerBound, int upperBound) {
        Random random = new Random(19);
        return  IntStream.range(0, numberOfMessages).boxed().distinct().map(it -> {
            Integer randomMessage = random.nextInt(upperBound - lowerBound) + lowerBound;
            return String.format("%09d", randomMessage);
        }).collect(Collectors.toList());
    }

    private List<String> generateUniqueMessages(int numberOfMessages) {
        List<String> randomMessages = IntStream.range(0, numberOfMessages).boxed().distinct().map(it -> {
            return String.format("%09d", it);
        }).collect(Collectors.toList());

        return randomMessages;
    }
}
