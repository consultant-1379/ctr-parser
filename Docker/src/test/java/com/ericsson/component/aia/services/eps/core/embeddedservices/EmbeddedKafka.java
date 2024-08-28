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
import java.util.Properties;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.component.aia.services.eps.core.util.TestUtility;
import com.ericsson.component.aia.services.eps.core.util.ZooKeeperStringSerializer;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import kafka.utils.ZkUtils;

/**
 * The <code>EmbeddedKafka</code> class run the kafka in embedded mode.
 */
public class EmbeddedKafka {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedKafka.class);

    public static final String KAFKA_TMP_DIR = "tmp_kafka_dir";
    /**
     * Zookeeper Timeout.
     */
    protected static final int ZK_TIMEOUT = 30000;
    /**
     * Kafka server.
     */
    protected KafkaServerStartable kafkaServer;
    /**
     * default kafka port which is randomized using findFreePort.
     */
    protected String kafkaPortNumber = "9092";
    /**
     * Default kafka host.
     */
    protected static final String KAFKA_HOST = "localhost";

    /**
     * @return the kafkaPortNumber
     */
    public String getKafkaPortNumber() {
        return kafkaPortNumber;
    }

    private EmbeddedZookeeper embeddedZookeeper;
    /**
     * Temporary directory.
     */
    private File tempDir;

    /**
     * Default constructor which take temporary directory as input where kafka stores its data.
     *
     * @param tempDir
     *            directory path under which sub-directory will be created with name {@value #KAFKA_TMP_DIR}
     */
    public EmbeddedKafka(final File tempDir) {
        super();
        this.tempDir = tempDir;
        embeddedZookeeper = new EmbeddedZookeeper(tempDir);
        kafkaPortNumber = String.valueOf(TestUtility.findFreePort());
        kafkaServer = new KafkaServerStartable(getKafkaBrokerConfiguration());
        kafkaServer.startup();
    }

    /**
     * @return kafka broker url.
     */
    public String getKafkaBrokerUrl() {
        return KAFKA_HOST + ":" + kafkaPortNumber;
    }

    /**
     * @return zookeeper connection url.
     */
    public String getZookeeperConnectionUrl() {
        return embeddedZookeeper.getZookeeperConnectString();
    }

    /**
     * Populate the kafka broker configuration.
     *
     * @return kakfa broker configuration.
     */
    protected KafkaConfig getKafkaBrokerConfiguration() {
        final Properties props = new Properties();
        props.put("broker.id", "0");
        props.put("host.name", KAFKA_HOST);
        props.put("port", kafkaPortNumber);
        props.put("log.dir", this.tempDir.getAbsolutePath() + File.separator + KAFKA_TMP_DIR);
        props.put("zookeeper.connect", embeddedZookeeper.getZookeeperConnectString());
        props.put("replica.socket.timeout.ms", "1500");
        props.put("auto.create.topics.enable", "false");
        props.put("default.replication.factor", "1");
        props.put("offsets.topic.replication.factor", "1");
        props.put("log.cleaner.enable", "false");
        final KafkaConfig config = new KafkaConfig(props);
        return config;
    }

    /**
     * Initialize ZkUtils utility.
     *
     * @return ZkUtils as per he config.
     */
    private ZkUtils getZkUtils() {
        LOG.trace("zookeeperConnectionString = {}", embeddedZookeeper.getZookeeperConnectString());
        final ZkClient creator = new ZkClient(embeddedZookeeper.getZookeeperConnectString(), ZK_TIMEOUT, ZK_TIMEOUT, new ZooKeeperStringSerializer());
        return ZkUtils.apply(creator, false);
    }

    /**
     * Create topic for test scenario.
     *
     * @param topic
     *            topic name
     * @param numberOfPartitions
     *            number of partitions
     * @param replicationFactor
     *            replication factor
     */
    public void createTopic(final String topic, final int numberOfPartitions, final int replicationFactor) {
        LOG.trace("Creating topic {}", topic);
        final ZkUtils zkUtils = getZkUtils();
        try {
            AdminUtils.createTopic(zkUtils, topic, numberOfPartitions, replicationFactor, new Properties(), RackAwareMode.Disabled$.MODULE$);
        } finally {
            zkUtils.close();
        }
        LOG.info("Topic {} create request is successfully posted", topic);
    }

    public void deleteTestTopic(final String topic) {
        final ZkUtils zkUtils = getZkUtils();
        try {
            LOG.info("Deleting topic {}", topic);
            AdminUtils.deleteTopic(zkUtils, topic);
        } finally {
            zkUtils.close();
        }
    }

    /**
     * Shutdown kafka server and embedded zookeeper service.
     */
    public void shutdownAndCleanUpServices() {
        LOG.info("Stoping embedded services associated with kafka.");
        kafkaServer.shutdown();
        kafkaServer.awaitShutdown();
        embeddedZookeeper.stop();
        embeddedZookeeper.close();
        LOG.info("Embedded services stop successfully. ");
    }

}
