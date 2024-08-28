/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.adc.handler.output_topic;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.ericsson.pm_event.PmEventOuterClass;

/**
 * Implementation for creating the Kafka output topics
 */
@Component
public class OutputTopicHandler {

    private static final String PM_EVENT_GROUP_VERSION = "pm_event_group_version";
    private static final String PM_EVENT_COMMON_VERSION = "pm_event_common_version";
    private static final String PM_EVENT_CORRECTION_VERSION = "pm_event_correction_version";
    private static final String EVENT_ID = "eventId";
    private static final String COMPUTE_NAME = "compute_name";
    private static final String NETWORK_MANAGED_ELEMENT = "network_managed_element";
    private static final String UNDERSCORE = "_";
    private static final String RADIONODE = "/radionode/";

    @Autowired
    private KafkaTemplate<String, Message<byte[]>> kafkaOutputTemplate;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Value("${spring.kafka.topics.output.partitions}")
    private int partitions;

    @Value("${spring.kafka.topics.output.replicas}")
    private short replicas;

    @Value("${spring.kafka.topics.output.compression-type}")
    private String compressionType;

    @Value("${spring.kafka.topics.output.name}")
    private String outputTopic;

    private static final Logger LOG = LoggerFactory.getLogger(OutputTopicHandler.class);

    /**
     * Construct the output topic names and create them in Kafka
     */
    public boolean setupOutputTopics() {
        final List<String> topicNames = fetchENMInstanceNames();
        buildAndCreateTopics(topicNames);

        return isTopicCreated(topicNames);
    }

    /**
     * Fetch the details for each ENM instance we need to connect to from the data catalog
     *
     * @return List of ENM names
     */
    protected List<String> fetchENMInstanceNames() {
        //Code here to be implemented in the next user story. Placeholder.
        return Arrays.asList(outputTopic);
    }

    /**
     * Build the output topics and creates them on kafka server
     *
     * @param topicNames
     *            string list of topics to be created on kafka server
     */
    protected void buildAndCreateTopics(final List<String> topicNames) {
        final NewTopic[] outputTopics = new NewTopic[topicNames.size()];
        for (int i = 0; i < topicNames.size(); i++) {
            outputTopics[i] = TopicBuilder.name(topicNames.get(i))
                    .partitions(partitions)
                    .replicas(replicas)
                    .config(TopicConfig.COMPRESSION_TYPE_CONFIG, compressionType)
                    .build();
        }

        kafkaAdmin.createOrModifyTopics(outputTopics);
    }

    /**
     * Does the output topic exist in Kafka
     *
     * @param topicNames
     *            string list of topic names to check if they have been created on kafka server
     * @return boolean to indicate the creation of topics
     */
    protected boolean isTopicCreated(final List<String> topicNames) {
        final Properties properties = new Properties();
        final String bootstrapServerAddress = kafkaAdmin.getConfigurationProperties().get("bootstrap.servers").toString();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerAddress);
        try (final AdminClient client = AdminClient.create(properties)) {
            final Set<String> existingTopics = client.listTopics().names().get();
            if (!existingTopics.containsAll(topicNames)) {
                LOG.debug("Topics were not created: {}", topicNames);
                return false;
            }
        } catch (final ExecutionException | InterruptedException e) {
            LOG.error("Error checking topics creation: ", e);
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    /**
     * Calculate which partition to write the Kafka message to on the topic
     *
     * @param nodeName
     *            string name of ENM Node from which the event file came
     * @return kafka partition
     * @throws NullPointerException
     */
    public int calculatePartition(final String nodeName) {
        int hashCode = nodeName.hashCode();
        if (hashCode < 0) {
            hashCode *= -1;
        }
        return hashCode % partitions;
    }

    /**
     * Publish the record to Kafka output topic
     *
     * @param pmEvent
     *            the decoded event to write to kafka topic
     * @param nodeName
     *            string name of ENM Node from which the event file came
     */
    public void sendKafkaMessage(final PmEventOuterClass.PmEvent pmEvent, final String nodeName) {
        final String node;
        if (nodeName.contains(RADIONODE)) {
            final String removePrefix = nodeName.split(RADIONODE)[1];
            final String[] arr = removePrefix.split(UNDERSCORE);
            node = arr[0] + "_" + arr[arr.length - 1];
        } else {
            final String[] arr = nodeName.split(UNDERSCORE);
            node = arr[0] + "_" + arr[arr.length - 1];
        }
        final int partition = calculatePartition(node);

        final Message<byte[]> message = MessageBuilder
                .withPayload(pmEvent.toByteArray())
                .setHeader(KafkaHeaders.TOPIC, outputTopic)
                .setHeader(KafkaHeaders.PARTITION_ID, partition)
                .setHeader(KafkaHeaders.MESSAGE_KEY, String.valueOf(pmEvent.getEventId()))
                .build();

        LOG.debug("Sending message={} to topic={} on partition={}", message, outputTopic, partition);
        kafkaOutputTemplate.send(message);
    }
}
