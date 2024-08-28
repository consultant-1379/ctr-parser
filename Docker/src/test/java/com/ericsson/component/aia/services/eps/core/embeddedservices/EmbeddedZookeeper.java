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
package com.ericsson.component.aia.services.eps.core.embeddedservices;

import java.io.File;
import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.component.aia.services.eps.core.util.TestUtility;

/**
 * The EmbeddedZookeeper configured dynamically allocating ephemeral ports by calling {@linkplain TestUtility#findFreePort()} utility. Temporary
 * sub-directory {@value #ZOOKEEPER_TMP_DIR} is created by {@code EmbeddedZookeeper} under provided directory location.
 *
 * To access the zookeeper connection URL refer {@link #getZookeeperConnectString()}.
 */
public class EmbeddedZookeeper {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedZookeeper.class);

    public static final String ZOOKEEPER_TMP_DIR = "tmp_zookeeper";

    /**
     * The instance holds the Zookeeper server.
     */
    private static TestingServer zkTestServer;

    /**
     * Default zookeeper port which is randomized using findFreePort utility in-order to avoid binding issue..
     */
    protected static int zkPort = 2181;

    /**
     * The constructor takes the location of temporary directory and creates sub-directory {@value #ZOOKEEPER_TMP_DIR}
     *
     * @param tempDir
     *            location of temp-dir
     */
    public EmbeddedZookeeper(final File tempDir) {
        super();
        zkPort = TestUtility.findFreePort();
        LOG.debug("The {} random port selected for zookeepr service", zkPort);
        try {
            zkTestServer = new TestingServer(zkPort, new File(tempDir.getAbsolutePath() + File.separator + ZOOKEEPER_TMP_DIR));
            LOG.info("Zookeeper created with connection URL {} ", getZookeeperConnectString());
        } catch (final Exception e) {
            LOG.error("Unable to create instance of embedded zookeeper ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @return zookeeper connection string.
     */
    public String getZookeeperConnectString() {
        return zkTestServer.getConnectString();
    }

    /**
     * Stop the server without deleting the temp directory
     */
    public void stop() {
        try {
            zkTestServer.stop();
        } catch (final IOException e) {
            LOG.error("Error while stoping zookeeper service ", e);
        }

    }

    /**
     * Close the server and any open clients and delete the temp directory
     */
    public void close() {
        try {
            zkTestServer.close();
        } catch (final IOException e) {
            LOG.error("Error while closing zookeeper service ", e);
        }

    }
}
