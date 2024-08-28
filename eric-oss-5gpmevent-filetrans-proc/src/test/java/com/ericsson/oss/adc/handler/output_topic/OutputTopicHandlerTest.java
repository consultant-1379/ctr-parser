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

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EmbeddedKafka(topics = {"5g-outputTopic.enm1"}, partitions = 3,
        brokerProperties = {"transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"})
class OutputTopicHandlerTest {

    @SpyBean
    private KafkaTemplate<String, Message<byte[]>> kafkaOutputTemplate;

    @Autowired
    OutputTopicHandler outputTopicHandler;

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Test
    @DisplayName("Should successfully setup the output topics")
    void test_setupOutputTopics() {
        boolean isCreated = outputTopicHandler.setupOutputTopics();

        assertTrue(isCreated);
    }

    @Test
    @DisplayName("Should successfully fetch the ENM instance details")
    void test_fetchENMInstanceNames() {
        List<String> expectedEnmInstanceNames = Arrays.asList("5g-outputTopic.enm1", "5g-outputTopic.enm2", "5g-outputTopic.enm3");

        assertEquals(expectedEnmInstanceNames, outputTopicHandler.fetchENMInstanceNames());
    }

    @Test
    @DisplayName("Should successfully build and create the topics on the embedded kafka server")
    void test_buildAndCreateTopics() {
        String enm1Topic = "5g-outputTopic.enm1";
        String enm2Topic = "5g-outputTopic.enm2";
        String enm3Topic = "5g-outputTopic.enm3";

        int expectedPartitions = 3;
        int expectedReplicas = 1;

        List<String> testTopicNames = Arrays.asList(enm1Topic, enm2Topic, enm3Topic);
        outputTopicHandler.buildAndCreateTopics(testTopicNames);
        Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(enm1Topic, enm2Topic, enm3Topic);

        assertNotNull(topics);
        assertEquals(expectedPartitions, topics.get(enm1Topic).partitions().size());
        assertEquals(expectedPartitions, topics.get(enm2Topic).partitions().size());
        assertEquals(expectedPartitions, topics.get(enm3Topic).partitions().size());
        assertEquals(expectedReplicas, topics.get(enm1Topic).partitions().get(0).replicas().size());
        assertEquals(expectedReplicas, topics.get(enm2Topic).partitions().get(0).replicas().size());
        assertEquals(expectedReplicas, topics.get(enm3Topic).partitions().get(0).replicas().size());
    }

    @Test
    @DisplayName("Should only add a topic once and not decrease the partitions")
    void test_buildAndCreateTopics_duplicateTopic() {
        String testTopic = "5g-outputTopic.enm99";
        int expectedPartitions = 3;
        int expectedReplicas = 1;
        List<String> testTopicNames = Collections.singletonList(testTopic);

        int duplicatePartitions = 2;
        short duplicateReplicas = 1;
        NewTopic duplicateTopic = new NewTopic(testTopic, duplicatePartitions, duplicateReplicas);

        outputTopicHandler.buildAndCreateTopics(testTopicNames);
        kafkaAdmin.createOrModifyTopics(duplicateTopic);
        Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(testTopic);

        assertNotNull(topics);
        assertEquals(expectedPartitions, topics.get(testTopic).partitions().size());
        assertEquals(expectedReplicas, topics.get(testTopic).partitions().get(0).replicas().size());

    }

    @Test
    @DisplayName("Should return true when the topic is created")
    void test_isTopicCreated_pass() {
        String enm1Topic = "5g-outputTopic.enm40";
        String enm2Topic = "5g-outputTopic.enm50";
        String enm3Topic = "5g-outputTopic.enm60";
        List<String> topics = Arrays.asList(enm1Topic, enm2Topic, enm3Topic);
        outputTopicHandler.buildAndCreateTopics(topics);
        assertTrue(outputTopicHandler.isTopicCreated(Arrays.asList(enm1Topic, enm2Topic, enm3Topic)));
    }

    @Test
    @DisplayName("Should return false when the topic is not created")
    void test_isTopicCreated_fails() {
        String enm1Topic = "5g-outputTopic.enm10";
        String enm2Topic = "5g-outputTopic.enm20";
        String enm3Topic = "5g-outputTopic.enm30";

        List<String> testTopicNames = Arrays.asList(enm1Topic, enm2Topic, enm3Topic);
        boolean isCreated = outputTopicHandler.isTopicCreated(testTopicNames);

        assertFalse(isCreated);
    }

    @Test
    @DisplayName("Should calculate the partition based on node name passed in and the number of kafka partitions configured")
    void test_calculatePartition() {
        String nodeName1 = "LTE98dg2ERBS00001";
        String nodeName2 = "LTE98dg2ERBS00002";
        String nodeName3 = "LTE98dg2ERBS00003";
        String nodeName4 = "LTE98dg2ERBS00004";

        int result1 = outputTopicHandler.calculatePartition(nodeName1);
        int result2 = outputTopicHandler.calculatePartition(nodeName2);
        int result3 = outputTopicHandler.calculatePartition(nodeName3);
        int result4 = outputTopicHandler.calculatePartition(nodeName4);

        assertEquals(0, result1);
        assertEquals(2, result2);
        assertEquals(1, result3);
        assertEquals(0, result4);
    }

    @Test
    @DisplayName("Should calculate the partition when node name is an empty string")
    void test_calculatePartition_empty_string() {
        int result = outputTopicHandler.calculatePartition("");

        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should throw NullPointerException when node name is null")
    void test_calculatePartition_NullPointerException() {
        assertThrows(NullPointerException.class,
                () -> outputTopicHandler.calculatePartition(null));
    }
}
