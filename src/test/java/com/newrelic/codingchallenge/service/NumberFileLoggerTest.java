package com.newrelic.codingchallenge.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.linesOf;


class NumberFileLoggerTest {
    @Test
    void log_numbers_to_file(@TempDir Path tempDir) throws Exception {
        File numberFile = tempDir.resolve("numbers.log").toFile();
        String[] testingValues = {"000000001", "000000002", "000000003"};
        try(NumberFileLogger numberFileLogger = new NumberFileLogger(numberFile)) {
            for (String expectedValue: testingValues) {
                numberFileLogger.logNumber(Integer.parseInt(expectedValue));
            }
        }
        assertThat(linesOf(numberFile)).containsExactly(
                testingValues
        );
    }

    @Test
    void logger_should_throw_initialization_exception(@TempDir Path tempDir) throws IOException {
        File numberFile = tempDir.resolve("numbers.log").toFile();
        numberFile.createNewFile();
        numberFile.setReadOnly();

        NumberFileLogger.NumberLoggerInitializationException exception = Assertions.assertThrows(NumberFileLogger.NumberLoggerInitializationException.class, () -> {
            String[] testingValues = {"000000001", "000000002", "000000003"};
            try(NumberFileLogger numberFileLogger = new NumberFileLogger(numberFile)) {
                for (String expectedValue: testingValues) {
                    numberFileLogger.logNumber(Integer.parseInt(expectedValue));
                }
            }
        });

        assertThat(exception.getMessage()).isEqualTo("Error creating a new " + numberFile.getAbsolutePath());
    }

}