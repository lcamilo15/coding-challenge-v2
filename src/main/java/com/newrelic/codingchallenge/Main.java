package com.newrelic.codingchallenge;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public class Main {
    public static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        logger.info("Starting new Relic Number Server App");
        String fileName = "numbers.log";
        int portNumber = 4000;
        int checkNumberTrackInSeconds = 10;
        int maxClients = 5;
        new NumberServerApp(Paths.get(fileName).toFile(), portNumber, checkNumberTrackInSeconds, maxClients).startServer();
    }
}