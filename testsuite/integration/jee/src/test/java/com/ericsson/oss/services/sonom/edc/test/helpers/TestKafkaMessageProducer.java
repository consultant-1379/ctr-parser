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

import com.ericsson.oss.services.sonom.common.kafka.producer.KafkaMessageProducer;

import java.util.Properties;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class TestKafkaMessageProducer pushes test messages onto the test kafka topic
 */
public class TestKafkaMessageProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestKafkaMessageProducer.class);

    private final KafkaMessageProducer<String, String> kafkaMessageProducer;

    public TestKafkaMessageProducer() {
        kafkaMessageProducer = new KafkaMessageProducer<>(TestEdcConstants.BOOTSTRAP_SERVER,
                StringSerializer.class.getName(),
                StringSerializer.class.getName(),
                addAdditionalProperties());
    }

    private static Properties addAdditionalProperties() {
        final Properties additionalProperties = new Properties();
        additionalProperties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        return additionalProperties;
    }

    public void sendMessage(final String data) {
        LOGGER.info("Sending input message to {} topic. DATA: {}", TestEdcConstants.INPUT_KAFKA_TOPIC, data);
        synchronized (this) {
            kafkaMessageProducer.sendKafkaMessage(TestEdcConstants.INPUT_KAFKA_TOPIC, null, data, (recordMetadata, e) -> {
                if (e == null) {
                    LOGGER.info("Records sent to {}", recordMetadata.topic());
                } else {
                    LOGGER.error("Exception while trying to send a record", e);
                }
            });
        }
    }

    public void stop() {
        kafkaMessageProducer.close();
    }

}
