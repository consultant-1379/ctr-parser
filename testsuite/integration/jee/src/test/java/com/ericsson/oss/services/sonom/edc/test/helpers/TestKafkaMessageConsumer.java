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

import com.ericsson.oss.services.sonom.common.kafka.consumer.KafkaRecordHandler;
import com.ericsson.oss.services.sonom.common.kafka.consumer.single.AutomaticCommitKafkaConsumer;
import com.ericsson.oss.services.sonom.common.kafka.exception.KafkaConsumerInstantiationException;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The class TestKafkaMessageConsumer consumes test messages from the test kafka topic
 */
public class TestKafkaMessageConsumer extends AutomaticCommitKafkaConsumer<String, byte[]> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestKafkaMessageConsumer.class);

    private static TestKafkaMessageConsumer singletonInstance;

    private final AtomicInteger numberOfReceivedEvents = new AtomicInteger(0);

    private TestKafkaMessageConsumer() throws KafkaConsumerInstantiationException {
        super(TestEdcConstants.BOOTSTRAP_SERVER,
                new StringDeserializer(),
                new ByteArrayDeserializer(),
                TestEdcConstants.GROUP_ID,
                Collections.singletonList(TestEdcConstants.OUTPUT_KAFKA_TOPIC),
                TestEdcConstants.POLL_TIMEOUT_IN_MS,
                addAdditionalProperties());
        super.consumeRecords();
    }

    public static TestKafkaMessageConsumer getInstance() throws KafkaConsumerInstantiationException {
        if (singletonInstance == null) {
            singletonInstance = new TestKafkaMessageConsumer();
        }
        return singletonInstance;
    }

    private static Properties addAdditionalProperties() {
        final Properties additionalProperties = new Properties();
        additionalProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        additionalProperties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, "KafkaMessageConsumer");
        additionalProperties.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
        return additionalProperties;
    }

    public int getNumberOfReceivedEvents() {
        return numberOfReceivedEvents.get();
    }

    @Override
    protected KafkaRecordHandler<ConsumerRecord<String, byte[]>> getKafkaRecordHandler() {
        return record -> {
            LOGGER.info("Received message: {}", record);

            if (record.value() == null) {
                LOGGER.error("Message published into '{}' Kafka topic has no content. Group Id: '{}'",
                        TestEdcConstants.OUTPUT_KAFKA_TOPIC, TestEdcConstants.GROUP_ID);
            } else {
                numberOfReceivedEvents.incrementAndGet();
                LOGGER.info("Number of events received from output topic: '{}'", numberOfReceivedEvents.get());
            }
        };
    }
}
