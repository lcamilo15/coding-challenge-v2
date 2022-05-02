package com.newrelic.codingchallenge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Implementation of NumberTrackerLogger,
 * Used to log Numbers into file
 */
public class NumberFileLogger implements NumberTrackerLogger, Closeable {
    String logFile;
    FileWriter fileWriter;
    Logger logger = LoggerFactory.getLogger(NumberFileLogger.class);

    public NumberFileLogger(File file) {
        this.logFile = file.getName();
        initFileWriter(file.getAbsolutePath());
    }
    @Override
    public void logNumber(Integer number) throws IOException {
        try {
            fileWriter.write(String.format("%09d%n", number));
            flush();
        } catch (IOException e) {
            logger.error("A problem occurred writing to file " + logFile, e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            fileWriter.close();
        } catch (IOException e) {
            logger.warn("A problem occurred closing the fileWriter", e);
            throw e;
        }
    }

    public void flush() throws IOException {
        try {
            fileWriter.flush();
        } catch (IOException e) {
            logger.warn("A problem occurred flushing to file " + logFile, e);
            throw e;
        }
    }

    protected void initFileWriter(String logFile) throws NumberLoggerInitializationException {
        try {
            this.fileWriter = new FileWriter(logFile, false);
        } catch (IOException e) {
            throw new NumberLoggerInitializationException("Error creating a new " + logFile, e);
        }
    }

    public static class NumberLoggerInitializationException extends RuntimeException {
        public NumberLoggerInitializationException(String message, IOException e) {
            super(message, e);
        }
    }
}
