package com.newrelic.codingchallenge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Implementation of NumberTrackerLogger,
 * Used to log Numbers into file
 */
public class NumberBufferedFileLogger implements NumberTrackerLogger, Closeable {
    String logFile;
    BufferedWriter bufferedWriter;
    Logger logger = LoggerFactory.getLogger(NumberBufferedFileLogger.class);
    int bufferSize;

    public NumberBufferedFileLogger(File file) {
        this(file, 200* 1000);
    }

    public NumberBufferedFileLogger(File file, int bufferSize) {
        this.logFile = file.getName();
        this.bufferSize = bufferSize;
        initFileWriter(file.getAbsolutePath());
    }
    @Override
    public void logNumber(Integer number) throws IOException {
        try {
            bufferedWriter.write(String.format("%09d%n", number));
        } catch (IOException e) {
            logger.error("A problem occurred writing to file " + logFile, e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            flush();
            bufferedWriter.close();
        } catch (IOException e) {
            logger.warn("A problem occurred closing the fileWriter", e);
            throw e;
        }
    }

    public void flush() throws IOException {
        try {
            bufferedWriter.flush();
        } catch (IOException e) {
            logger.warn("A problem occurred flushing to file " + logFile, e);
            throw e;
        }
    }

    protected void initFileWriter(String logFile) throws NumberLoggerInitializationException {
        try {
            this.bufferedWriter = new BufferedWriter(new FileWriter(logFile, false), bufferSize);
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
