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

import java.util.concurrent.TimeUnit;

public final class TestEdcConstants {
    /**
     * Kafka Constants
     */
    public static final String INPUT_KAFKA_TOPIC = "celltraceInputFiles5G";
    public static final String OUTPUT_KAFKA_TOPIC = "celltraceDecoded5G";
    public static String BOOTSTRAP_SERVER = System.getenv("KAFKA_BOOTSTRAP_SERVER");;
    public static final String GROUP_ID = "group_id";
    public static final long POLL_TIMEOUT_IN_MS = TimeUnit.SECONDS.toMillis(2);

    /**
     * EDC Constants
     */
    public static final String EDC_SERVICE = "eric-oss-5gpmevent-filetrans-proc";
    public static final String EDC_HC_ENDPOINT = "/actuator/health";
    public static final String EDC_PORT = "8080";

    /**
     * CTR File Constants
     */
    private static final String TEST_FILE_PATH = "/tmp/files/";
    private static final String CUCP_FILE_NAME = "A20211118.0330-0345_CellTrace_CUCP0_1_1_V0026860EN077162.gpb.gz";
    private static final String DU_FILE_NAME = "A20211118.0330-0345_CellTrace_DU0_1_1_V0026860EN077162.gpb.gz";
    public static final String TEST_CUCP_INPUT_MESSAGE = "{ \"path\": \""+ TEST_FILE_PATH + CUCP_FILE_NAME + "\" }";
    public static final String TEST_DU_INPUT_MESSAGE = "{ \"path\": \""+ TEST_FILE_PATH + DU_FILE_NAME + "\" }";
}
