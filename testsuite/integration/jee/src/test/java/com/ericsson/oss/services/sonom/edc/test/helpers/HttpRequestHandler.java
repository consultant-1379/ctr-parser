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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The class HttpRequestHandler contains functions necessary to query HTTP requests for testing
 */
public class HttpRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestHandler.class);

    private HttpRequestHandler() {

    }

    /**
     * This function will query an endpoint using the get method until it returns a 200 OK or reaches the max retry
     * count
     * @param service
     *          the hostname for the request
     * @param port
     *          the port the hostname will expect the request on
     * @param healthCheckEndpoint
     *          the endpoint to query for the health check
     * @param retryCount
     *          the amount of times to retry before giving up
     * @param retryTimeoutInSeconds
     *          the amount of time to wait between retries
     * @return the last response code retried from the service.
     */
    public static int waitForServiceToBeRunning(final String service, final String port,
                                                final String healthCheckEndpoint,
                                                final int retryCount, final int retryTimeoutInSeconds) {
        int responseCode = 0;
        for (int i = 0; i < retryCount; i++) {
            try {
                final String url = "http://" + service + ":" + port + healthCheckEndpoint;
                LOGGER.info("Attempting to communicate with {}", url);
                final HttpURLConnection connection = buildUrlConnection(url);
                responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    LOGGER.info("Healthcheck returned 200 OK for {}", url);
                    return responseCode;
                }

            } catch (final IOException e) {
                LOGGER.error("Failed to sent request to Service due to IO Exception", e);
            }

            SleepHandler.sleep(retryTimeoutInSeconds, "Service Startup");
        }
        return responseCode;
    }

    private static HttpURLConnection buildUrlConnection(final String url) throws IOException {
        final URL obj = new URL(url);
        final HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(60);
        return connection;
    }
}
