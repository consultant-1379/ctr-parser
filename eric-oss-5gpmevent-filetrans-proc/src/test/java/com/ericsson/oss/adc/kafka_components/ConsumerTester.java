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

package com.ericsson.oss.adc.kafka_components;

import com.ericsson.oss.adc.models.DecodedEvent;
import com.ericsson.oss.adc.models.OutputMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Kafka consumer for testing purposes
 */
@Component
public class ConsumerTester {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServerConfig;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupIdConsumerConfig;

    @Value("${spring.kafka.topics.input.partition.assignment.strategy}")
    private String partitionAssignmentStrategy;

    @Value("${spring.kafka.topics.input.session.timeout.ms}")
    private int sessionTimeoutMs;

    private static final String TOPIC =  "5g-outputTopic.enm1";
    private static final String PM_EVENT_GROUP_VERSION = "pm_event_group_version";
    private static final String PM_EVENT_COMMON_VERSION = "pm_event_common_version";
    private static final String PM_EVENT_CORRECTION_VERSION = "pm_event_correction_version";
    private static final String EVENT_ID = "eventId";
    private static final String COMPUTE_NAME = "compute_name";
    private static final String NETWORK_MANAGED_ELEMENT = "network_managed_element";

    private CountDownLatch latch = new CountDownLatch(1);
    private DecodedEvent eventData = null;
    private int recordCount = 0;

    @Bean
    public ConsumerFactory<String, String> consumerOutputFactory() {

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupIdConsumerConfig);
        config.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, partitionAssignmentStrategy);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        //Both "enable auto commit config=false" and "isolation level config= read_committed"" need to be set for transactions to work downstream
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> consumerKafkaListenerOutputContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerOutputFactory());
        return factory;
    }

    @KafkaListener(containerFactory="consumerKafkaListenerOutputContainerFactory", topics = TOPIC)
    public void receive( @Payload String message,
                         @Header(PM_EVENT_GROUP_VERSION) long pmEventGroupVersion,
                         @Header(PM_EVENT_COMMON_VERSION) long pmEventCommonVersion,
                         @Header(PM_EVENT_CORRECTION_VERSION) long pmEventCorrectionVersion,
                         @Header(EVENT_ID) String eventId,
                         @Header(COMPUTE_NAME) String computeName,
                         @Header(NETWORK_MANAGED_ELEMENT) String networkManagedElement ) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        OutputMessage payload = objectMapper.readValue(message, OutputMessage.class);

        eventData = new DecodedEvent(
                pmEventGroupVersion,
                pmEventCommonVersion,
                pmEventCorrectionVersion,
                eventId,
                computeName,
                networkManagedElement,
                payload.getPayload()
        );

        recordCount++;
        latch.countDown();
    }

    /**
     * Sets countdown latch
     * @param count
     */
    public void setCountDownLatch(int count) {
        latch = new CountDownLatch(count);
    }

    /**
     * Returns countdown latch
     * @return
     */
    public CountDownLatch getLatch() {
        return latch;
    }

    /**
     * Returns count for number of records that have been consumed
     * @return
     */
    public int getRecordCount() {
        return recordCount;
    }

    /**
     * Returns event data consumed
     * @return
     */
    public DecodedEvent getEventData() {
        return eventData;
    }

    /**
     * Resets consumer variables
     */
    public void reset(){
        latch = new CountDownLatch(1);
        eventData = null;
        recordCount = 0;
    }
}
