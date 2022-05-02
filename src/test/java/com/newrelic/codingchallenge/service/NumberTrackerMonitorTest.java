package com.newrelic.codingchallenge.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberTrackerMonitorTest {
    @Mock
    NumberTracker numberTracker;
    @Test
    public void number_monitor_should_runs_as_scheduled() throws Exception {
        int periodInMilliseconds = 10;
        int howManyTimesShouldBeCalled = 10;
        doReturn(new NumberTracker.CountSnapShot(0,0,0)).when(numberTracker).clearAndGetCountSnapShot();
        NumberTrackerMonitor numberTrackerMonitor = new NumberTrackerMonitor(periodInMilliseconds, numberTracker);
        numberTrackerMonitor.start();
        Thread.sleep((howManyTimesShouldBeCalled * periodInMilliseconds));
        verify(numberTracker, atLeast(howManyTimesShouldBeCalled)).clearAndGetCountSnapShot();
    }

}