/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.component.aia.services.eps.core.util;

import java.io.File;

/**
 * The <code>Constants</code> specify all the common constants use in test activity.
 */
public interface Constants {
    /**
     * List of velocity template related path.
     */
    String FLOW_VELOCITY_TEMPLATE = "/template" + File.separator + "flow.vm";
    String IPL_PUBLISHER_VELOCITY_TEMPLATE = "/template" + File.separator + "publisher_ipl.vm";
    String IPL_SUBSCRIBER_VELOCITY_TEMPLATE = "/template" + File.separator + "subscriber_ipl.vm";
    String EVENT_FILTER_VELOCITY_TEMPLATE = "/template" + File.separator + "eventfilter.vm";

    String AVRO_SCHEMA_DIR = "src/test/resources/schemas/avro/";

    /**
     * List of test files
     */
    String TEST_FILE_PATH = "src/test/resources/data/ctr_file/testfile/A20180531.0130-0700-0145-0700_SubNetwork=ONRM_ROOT_MO,SubNetwork=MKT_041,MeContext=041409_APPLEWOOD_celltracefile_DUL1_1.bin.gz";
    String TEST_RESUTL_FILE_FILTER = ".csv";
    String CORRUPT_TEST_FILE_PATH = "src/test/resources/data/ctr_file/corruptfile/A20180531.0215-0700-0230-0700_SubNetwork=ONRM_ROOT_MO,SubNetwork=MKT_045,MeContext=045510_RANCHO_VERDE_celltracefile_DUL1_1.bin.gz";

    /**
     * List of JMX Metrics
     */
    String JMX_PM_FILE_PARSER_EVENTS_PROCESSED = "PMFileParser@eventsProcessed";
    String JMX_PM_FILE_PARSER_ERRONEOUS_FILES = "PMFileParser@erroneousFiles";
    String JMX_PM_FILE_PARSER_FILE_COUNTS = "PMFileParser@filecounts";
    String JMX_PM_FILE_PARSER_IGNORED_EVENTS = "PMFileParser@ignoredEvents";
    String JMX_METER_ATTRIBUTE_NAME = "Count";
}
