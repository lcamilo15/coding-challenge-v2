package com.newrelic.codingchallenge.service;

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
            System.out.println(MessageFormat.format("Received {0} unique numbers, {1} duplicates. Unique total: {2}", countSnapShot.uniqueNumbers, countSnapShot.duplicateNumbers, countSnapShot.uniqueTotal));
        }, periodInMilliseconds, periodInMilliseconds, TimeUnit.MILLISECONDS);
    }
    public void shutDown() {
        scheduledExecutorService.shutdown();
    }
}
