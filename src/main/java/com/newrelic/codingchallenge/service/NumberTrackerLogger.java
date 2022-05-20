package com.newrelic.codingchallenge.service;

import java.io.Closeable;
import java.io.IOException;

public interface NumberTrackerLogger extends Closeable {
    public void logNumber(Integer number) throws IOException;

}
