package com.newrelic.codingchallenge;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class MemoryUtils {
    static public long getGcCount() {
        long sum = 0;
        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = b.getCollectionCount();
            if (count != -1) { sum += count; }
        }
        return sum;
    }

    static public long getReallyUsedMemory()  {
        long before = getGcCount();
        int timeoutInSeconds = 1;
        LocalDateTime startTime = LocalDateTime.now();
        System.gc();
        while (getGcCount() == before && LocalDateTime.now().until(startTime, ChronoUnit.SECONDS) < timeoutInSeconds) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return getCurrentlyAllocatedMemory();
    }

    static public long getCurrentlyAllocatedMemory() {
        final Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    public static class MemorySnapShot {
        long startMemUsage;
        long endMemUsage;

        LocalDateTime startTime;
        LocalDateTime endTime;

        Map<String, Map<String, LocalDateTime>> timeTrack = new HashMap<>();

        public MemorySnapShot() {
            startMemUsage = getReallyUsedMemory();
            endMemUsage = startMemUsage;
            startTime = LocalDateTime.now();
            endTime = LocalDateTime.now();
        }
        public void captureMemUsage() {
            endMemUsage = getReallyUsedMemory();
            endTime = LocalDateTime.now();
        }

        public void trackTime(String name) {
            if (timeTrack.containsKey(name)) {
                Map<String, LocalDateTime> timeElapsed = new HashMap<>();
                timeElapsed.put("START_TIME", LocalDateTime.now());
                timeElapsed.put("END_TIME", LocalDateTime.now());
                timeTrack.put(name, timeElapsed);
            } else {
                timeTrack.get(name).put("END_TIME", LocalDateTime.now());
            }
        }

        public long getTimeElapsed(String name) {
            if (timeTrack.containsKey(name)) {
                 return timeTrack.get(name).get("START_TIME").until(timeTrack.get(name).get("START_TIME"), ChronoUnit.SECONDS);
            }
            return 0;
        }

        public long getTimeElapsed() {
            return startTime.until(endTime, ChronoUnit.SECONDS);
        }
    }
}
