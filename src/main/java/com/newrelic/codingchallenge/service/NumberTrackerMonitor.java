package com.newrelic.codingchallenge.service;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NumberTrackerMonitor {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final int periodInMilliseconds;
    private final NumberTracker numberTracker;
    public NumberTrackerMonitor(int periodInMilliseconds, NumberTracker numberTracker) {
        this.periodInMilliseconds = periodInMilliseconds;
        this.numberTracker = numberTracker;
    }
    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            NumberTracker.CountSnapShot countSnapShot = numberTracker.clearAndGetCountSnapShot();
            System.out.println(String.format("Received %d unique numbers, %d duplicates. Unique total: %d", countSnapShot.uniqueNumbers, countSnapShot.duplicateNumbers, countSnapShot.uniqueTotal));
        }, periodInMilliseconds, periodInMilliseconds, TimeUnit.MILLISECONDS);
    }
    public void shutDown() throws IOException {
        numberTracker.close();
        scheduledExecutorService.shutdown();
    }
}
