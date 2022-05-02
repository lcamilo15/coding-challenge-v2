package com.newrelic.codingchallenge.service;

import java.io.IOException;

public interface NumberTrackerLogger {
    public void logNumber(Integer number) throws IOException;
}
