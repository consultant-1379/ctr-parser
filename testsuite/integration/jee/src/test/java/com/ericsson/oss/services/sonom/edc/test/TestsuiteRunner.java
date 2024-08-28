/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */
package com.ericsson.oss.services.sonom.edc.test;

import java.util.Objects;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.StoppedByUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs all tests defined in {@link IntegrationTests }.
 */
public class TestsuiteRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestsuiteRunner.class);

    private TestsuiteRunner() {
    }

    /**
     * Testsuite uber jar will start from this main().
     * <p>
     * Collects all integration test defined in {@link IntegrationTests } and executes it.
     * <p>
     * If all tests were successful, it exits with status 0, else with status 1.
     */
    public static void main(final String[] args) {
        LOGGER.info("Running tests!");
        Result testsResult = null;

        try {
            testsResult = JUnitCore.runClasses(IntegrationTests.class);
        } catch (final StoppedByUserException e) {
            LOGGER.info("One of the tests failed therefore the testing was aborted.");
            handleFailure();
        }

        if (Objects.nonNull(testsResult) && testsResult.wasSuccessful()) {
            LOGGER.info("Tests completed successfully :)");
            System.exit(0); // NOPMD System.exit needed for test class only
        }

        handleFailure();
    }

    private static void handleFailure() {
        LOGGER.info("Tests completed with failures :(");
        System.exit(1); // NOPMD System.exit needed for test class only
    }

}
