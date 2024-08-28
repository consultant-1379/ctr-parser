/*******************************************************************************
 * COPYRIGHT Ericsson 2021 - 2022
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

package com.ericsson.oss.adc.controller.topiclistener;

import java.io.IOException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.ericsson.oss.adc.file_processor.FileProcessor;
import com.ericsson.oss.adc.models.InputMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation for listening to 5G ENM event file notification topic to consume event files
 */
@Component
public class InputTopicListenerImpl {
    /**
     * Logger for the class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(InputTopicListenerImpl.class);

    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;

    FileProcessor fileProcessor;

    @Autowired
    public InputTopicListenerImpl(final FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    /**
     * Listener for the {@link InputTopicListenerImpl#inputTopicName} topic. When a notification message is received an event file will be processed
     * and decoded.
     *
     * @param consumerRecord
     *            A {@link ConsumerRecord} holding the {@link InputMessage}.
     * @param acknowledgment
     *            An {@link Acknowledgment} to be used to manually commit the offset when processing completes.
     * @throws JsonProcessingException
     *             Should only occur if payload is not valid JSON to be deserialized to {@link InputMessage}
     */
    @KafkaListener(containerFactory = "consumerKafkaListenerContainerFactory", topics = "${spring.kafka.topics.input.name}", autoStartup = "true")
    public void listen(final ConsumerRecord<String, String> consumerRecord, final Acknowledgment acknowledgment) throws JsonProcessingException {
        LOG.info("Message received off Kafka topic {} : {}", inputTopicName, consumerRecord);

        final ObjectMapper objectMapper = new ObjectMapper();
        final InputMessage message = objectMapper.readValue(consumerRecord.value(), InputMessage.class);

        try {
            fileProcessor.processEventFile(message);
            LOG.info("Acknowledging {}", message.getPath());
            acknowledgment.acknowledge();
        } catch (final IOException e) {
            LOG.error("Error processing file, transaction aborted:", e);
        }
    }
}
