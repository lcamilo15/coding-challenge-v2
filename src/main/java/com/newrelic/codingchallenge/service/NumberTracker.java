package com.newrelic.codingchallenge.service;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NumberTracker {
    protected final Set<Integer> indexedNumbers = ConcurrentHashMap.newKeySet();
    protected final AtomicInteger uniqueNumbers = new AtomicInteger();
    protected final AtomicInteger duplicateNumbers = new AtomicInteger();
    private final NumberTrackerLogger numberTrackerLogger;

    public NumberTracker(NumberTrackerLogger numberTrackerLogger) {
        this.numberTrackerLogger = numberTrackerLogger;
    }

    public Set<Integer> getIndexedNumbers() {
        return indexedNumbers;
    }

    public void trackNumber(Integer number) {
        synchronized (indexedNumbers) {
            if (indexedNumbers.add(number)) {
                uniqueNumbers.incrementAndGet();
                try {
                    numberTrackerLogger.logNumber(number);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                duplicateNumbers.incrementAndGet();
            }
        }
    }

    public CountSnapShot clearAndGetCountSnapShot() {
        synchronized (indexedNumbers) {
            return new CountSnapShot(uniqueNumbers.getAndSet(0), duplicateNumbers.getAndSet(0), indexedNumbers.size());
        }
    }

    public static class CountSnapShot {
        public final int uniqueNumbers;
        public final int duplicateNumbers;
        public final int uniqueTotal;
        public CountSnapShot(int uniqueNumbers, int duplicateNumbers, int uniqueTotal) {
            this.uniqueNumbers = uniqueNumbers;
            this.duplicateNumbers = duplicateNumbers;
            this.uniqueTotal = uniqueTotal;
        }
    }
}