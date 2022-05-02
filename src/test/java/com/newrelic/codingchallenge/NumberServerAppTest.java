package com.newrelic.codingchallenge;

import com.newrelic.codingchallenge.service.NumberTracker;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.newrelic.codingchallenge.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NumberServerAppTest {
    Integer freePort = findFreePort();
    Integer maxClients = 5;
    Integer extraConnectionsExpectedToBeRejected = 10;
    int checkNumberTrackInSeconds = 1;
    ThreadPoolExecutor executor;
    NumberServer numberServer;
    NumberServerApp numberServerApp;
    File tempLoggingFile;

    @BeforeEach
    public void initServer(@TempDir Path tempDir) {
        tempLoggingFile = tempDir.resolve("numbers.log").toFile();
        numberServer = Mockito.spy(new NumberServer(freePort, maxClients));
        numberServerApp = new NumberServerApp(tempLoggingFile, freePort, checkNumberTrackInSeconds, maxClients, numberServer);
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1000);
        executor.execute(numberServerApp::startServer);
    }

    @AfterEach
    public void stopServer() {
        numberServer.shutdown();
        executor.shutdown();
    }

    @Test
    void extra_client_connection_should_be_rejected() {
        //Generate List of Random Numbers for testing Number Tracker
        List<CompletableFuture<String>> clientMessages = IntStream.range(0, maxClients  + extraConnectionsExpectedToBeRejected ).mapToObj(it -> CompletableFuture.supplyAsync(() -> {
            while(!numberServer.isRunning()) TestUtils.sleep(100);
            String randomNumber = String.format("%09d", ThreadLocalRandom.current().nextInt(1, 999999991));
            sendMessageToSever(freePort, randomNumber, 100);
            return randomNumber;
        }, executor)).collect(Collectors.toList());

        CompletableFuture[] completableFutures = clientMessages.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(completableFutures).join();
        verify(numberServer, times(extraConnectionsExpectedToBeRejected)).reject(Mockito.any());;
    }

    @Test
    void valid_digits_should_be_logged_into_file() throws IOException {
        List<CompletableFuture<String>> clientMessages = IntStream.range(1, maxClients).mapToObj(it->CompletableFuture.supplyAsync(() -> {
            while(!numberServer.isRunning()) TestUtils.sleep(100);
            String randomNumber = String.format("%09d", it);
            sendMessageToSever(freePort, randomNumber, 100);
            return randomNumber;
        })).collect(Collectors.toList());

        //Send lit of invalid messages
        IntStream.range(1, maxClients).mapToObj(it->CompletableFuture.supplyAsync(() -> {
            while(!numberServer.isRunning()) TestUtils.sleep(100);
            sendMessageToSever(freePort, "INVALID_MESSAGE", 100);
            return "INVALID_MESSAGE";
        })).collect(Collectors.toList());

        List<String> validMessagesSent = clientMessages.stream().map(it -> it.join().replaceAll("\\n","")).collect(Collectors.toList());
        List<String> validMessagesLogged = FileUtils.readLines(tempLoggingFile, StandardCharsets.UTF_8);
        assertThat(validMessagesSent).hasSameElementsAs(validMessagesLogged);
    }

    @Test
    void invalid_messages_shall_be_dropped() throws IOException, ExecutionException, InterruptedException {
        List<CompletableFuture<String>> clientMessages = IntStream.range(0, maxClients).mapToObj(it->CompletableFuture.supplyAsync(() -> {
            while(!numberServer.isRunning()) TestUtils.sleep(100);
            sendMessageToSever(freePort, "INVALID_MESSAGE", 100);
            return "INVALID_MESSAGE";
        })).collect(Collectors.toList());
        CompletableFuture[] completableFutures = clientMessages.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(completableFutures).get();
        verify(numberServer, times(maxClients)).closeGracefully(Mockito.any());;
    }

    @Test
    void test_number_tracker() throws IOException, ExecutionException, InterruptedException {

        List<CompletableFuture<String>> clientMessages = IntStream.range(0, maxClients).mapToObj(it->CompletableFuture.supplyAsync(() -> {
            while(!numberServer.isRunning()) TestUtils.sleep(10);
            String randomNumber = String.format("%09d", it);
            sendMessageToSever(freePort, randomNumber, 10);
            return randomNumber;
        })).collect(Collectors.toList());

        List<String> uniqueMessagesSent = clientMessages.stream().map(CompletableFuture::join).collect(Collectors.toList());
        NumberTracker.CountSnapShot snapShotFirstCall = numberServerApp.numberTracker.clearAndGetCountSnapShot();
        sleep(10);

        List<CompletableFuture<String>> duplicateMessages = IntStream.range(0, maxClients).mapToObj(it->CompletableFuture.supplyAsync(() -> {
            while(!numberServer.isRunning()) TestUtils.sleep(10);
            String randomNumber = String.format("%09d", it);
            sendMessageToSever(freePort, randomNumber, 10);
            return randomNumber;
        })).collect(Collectors.toList());

        List<String> duplicateNumbersSent = duplicateMessages.stream().map(CompletableFuture::join).collect(Collectors.toList());
        NumberTracker.CountSnapShot snapShotDuplicates = numberServerApp.numberTracker.clearAndGetCountSnapShot();
        sleep(10);

        List<CompletableFuture<String>> newClientMessages = IntStream.range(0, maxClients).mapToObj(it->CompletableFuture.supplyAsync(() -> {
            while(!numberServer.isRunning()) TestUtils.sleep(10);
            String randomNumber = String.format("%09d", it+10);
            sendMessageToSever(freePort, randomNumber, 10);
            return randomNumber;
        })).collect(Collectors.toList());

        List<String> newClientMessagesSent = newClientMessages.stream().map(CompletableFuture::join).collect(Collectors.toList());
        NumberTracker.CountSnapShot snapShotNewClientMessagesSent = numberServerApp.numberTracker.clearAndGetCountSnapShot();

        assertThat(snapShotFirstCall.uniqueNumbers).isEqualTo(maxClients);
        assertThat(snapShotFirstCall.uniqueTotal).isEqualTo(maxClients);
        assertThat(snapShotFirstCall.duplicateNumbers).isEqualTo(0);

        assertThat(snapShotDuplicates.uniqueNumbers).isEqualTo(0);
        assertThat(snapShotDuplicates.uniqueTotal).isEqualTo(maxClients);
        assertThat(snapShotDuplicates.duplicateNumbers).isEqualTo(maxClients);

        assertThat(snapShotNewClientMessagesSent.uniqueNumbers).isEqualTo(maxClients);
        assertThat(snapShotNewClientMessagesSent.uniqueTotal).isEqualTo(maxClients * 2);
        assertThat(snapShotNewClientMessagesSent.duplicateNumbers).isEqualTo(0);
    }
}