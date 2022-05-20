package com.newrelic.codingchallenge;

import com.newrelic.codingchallenge.service.NumberBufferedFileLogger;
import com.newrelic.codingchallenge.service.NumberTracker;
import com.newrelic.codingchallenge.service.NumberTrackerMonitor;

import java.io.File;
import java.io.IOException;

import static com.newrelic.codingchallenge.MessageUtils.isTerminationString;
import static com.newrelic.codingchallenge.MessageUtils.isValidNumber;

/**
 * Server implementation following rules for New Relic Coding Challenge.
 */
public class NumberServerApp {
    private File logFile;
    private int portNumber;
    private int checkNumberTrackInSeconds;
    private int maxClients;
    NumberServer numberServer;

    NumberTracker numberTracker;

    /**
     * @param logFile file for which to log valid numbers from incoming messages
     * @param portNumber port number for which to run server
     * @param checkNumberTrackInSeconds number of seconds the Tracker should check for messages
     * @param maxClients max number of clients
     */
    public NumberServerApp(File logFile, int portNumber, int checkNumberTrackInSeconds, int maxClients) {
        this(logFile, portNumber, checkNumberTrackInSeconds, maxClients, new NumberServer(portNumber, maxClients),  new NumberTracker(new NumberBufferedFileLogger(logFile)));
    }

    /**
     * @param logFile file for which to log valid numbers from incoming messages
     * @param portNumber port number for which to run server
     * @param checkNumberTrackInSeconds number of seconds the Tracker should check for messages
     * @param maxClients max number of clients
     * @param numberServer instance of number server, useful for testing
     */
    public NumberServerApp(File logFile, int portNumber, int checkNumberTrackInSeconds, int maxClients, NumberServer numberServer, NumberTracker numberTracker) {
        this.logFile = logFile;
        this.portNumber = portNumber;
        this.checkNumberTrackInSeconds = checkNumberTrackInSeconds;
        this.maxClients = maxClients;
        this.numberServer = numberServer;
        this.numberTracker = numberTracker;
    }


    public void startServer() {


        NumberTrackerMonitor numberTrackerMonitor = new NumberTrackerMonitor(checkNumberTrackInSeconds * 1000, numberTracker);

        numberServer.onConnect((serverConnection)->{

            serverConnection.onIncomingMessage(message->{
                /**
                 * Numbers presented to the Application must include leading zeros as necessary to ensure
                 * they are each 9 decimal digits.
                 */
                if (isValidNumber(message)) {
                    numberTracker.trackNumber(Integer.parseInt(message));
                    return;
                }

                /**
                 * If any connected client writes a single line with only the word "terminate" followed by a
                 * server-native newline sequence, the Application must close all client connections and perform
                 * a clean shutdown as quickly as possible.
                 */
                if (isTerminationString(message)) {
                    numberServer.shutdown();
                    return;
                }
                serverConnection.close();

            });
        });

        numberServer.onShutDown((it)->{
            try {
                numberTrackerMonitor.shutDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        numberTrackerMonitor.start();
        numberServer.start();
    }
}
