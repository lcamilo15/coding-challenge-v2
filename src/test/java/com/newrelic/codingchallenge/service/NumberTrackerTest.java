package com.newrelic.codingchallenge.service;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class NumberTrackerTest {

    @Test
    public void test_number_tracker() throws ExecutionException, InterruptedException {
        int numberOfThreads = 100;
        int numberOfMessagesPerThread = 100;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numberOfThreads);

        AtomicInteger numbersTracked = new AtomicInteger(0);
        Set<Integer> uniqueValues = ConcurrentHashMap.newKeySet();

        NumberTracker numberTracker = new NumberTracker(number -> {
            numbersTracked.incrementAndGet();
        });


        //Generate List of Random Numbers for testing Number Tracker
        List<CompletableFuture<Object>> randomThreads = IntStream.range(0, numberOfThreads).mapToObj(it -> CompletableFuture.supplyAsync(() -> {
            for (int index = 0; index <= numberOfMessagesPerThread; index++) {
                int numberToTrack = RandomUtils.nextInt(1, 100);
                uniqueValues.add(numberToTrack);
                numberTracker.trackNumber(numberToTrack);
                try {
                    Thread.sleep(RandomUtils.nextInt(1, 10));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }, executor)).collect(Collectors.toList());

        CompletableFuture[] completableFutures = randomThreads.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(completableFutures).get();
        assertThat(numbersTracked.get()).isEqualTo(uniqueValues.size());
        assertThat(uniqueValues).isEqualTo(numberTracker.getIndexedNumbers());
    }
}