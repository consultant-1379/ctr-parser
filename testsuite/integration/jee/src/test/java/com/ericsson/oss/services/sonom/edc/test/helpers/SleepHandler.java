/*
 * *------------------------------------------------------------------------------
 * ******************************************************************************
 *  COPYRIGHT Ericsson 2022
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.oss.services.sonom.edc.test.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * The class SleepHandler contains utilities for sleeping during tests
 */
public class SleepHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepHandler.class);

    private SleepHandler() {

    }

    /**
     * This function sleeps for a specific amount of time if possible
     * @param timeInSeconds
     *          amount of time to sleep for
     * @param reason
     *          used to log a message before sleep starts
     */
    public static void sleep(final int timeInSeconds, final String reason) {
        try {
            LOGGER.info("Sleeping for {}s to allow for {}", timeInSeconds, reason);
            TimeUnit.SECONDS.sleep(timeInSeconds);
        } catch (InterruptedException e) {
            LOGGER.error("Sleep Interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
