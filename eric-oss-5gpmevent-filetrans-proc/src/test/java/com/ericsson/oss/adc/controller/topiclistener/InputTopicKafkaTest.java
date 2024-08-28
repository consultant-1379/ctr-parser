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
package com.ericsson.oss.adc.controller.topiclistener;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import com.ericsson.oss.adc.file_processor.FileProcessor;

@SpringBootTest
@EmbeddedKafka
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InputTopicKafkaTest {

    private static final Logger LOG = LoggerFactory.getLogger(InputTopicKafkaTest.class);
    private static final int FOUR_SECONDS_IN_MS = 4000;

    @Autowired
    private KafkaListenerEndpointRegistry registry; // Need to start our listener for test as it doesn't start automatically

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockBean
    private FileProcessor fileProcessorMock;

    @SpyBean
    private InputTopicListenerImpl inputTopicListenerImpl;

    @Captor
    private ArgumentCaptor<Acknowledgment> acknowledgmentArgumentCaptor;

    @Value("${spring.kafka.topics.input.name}")
    private String topicName;

    // Just needs to be deserializable message, contents irrelevant as fileProcessor is mocked.
    private static final String jsonInputTopicMessage0 = "{ \"nodeName\": \"testNode0\", \"fileLocation\": " +
            "\"src/test/resources/test-gz-files/5G-event-file-930.gpb" +
            ".gz\"}";
    private static final String jsonInputTopicMessage1 = "{ \"nodeName\": \"testNode1\", \"fileLocation\": " +
            "\"src/test/resources/test-gz-files/5G-event-file-930.gpb" +
            ".gz\"}";
    private static final String jsonInputTopicMessage2 = "{ \"nodeName\": \"testNode2\", \"fileLocation\": " +
            "\"src/test/resources/test-gz-files/5G-event-file-930.gpb" +
            ".gz\"}";

    private Producer<String, String> producer;

    @BeforeAll
    void setUpClass() {
        toggleListeners(true);
        createProducer();
    }

    @Test
    @Order(1)
    @DisplayName("1 Should run the test and acknowledge so listener and mock should only be called once")
    public void the_acknowledge_file_processed() throws IOException, InterruptedException {

        doNothing().when(fileProcessorMock).processEventFile(any());

        sendToInputTopic(createProducerRecord(jsonInputTopicMessage0));

        TimeUnit.SECONDS.sleep(10); // Wait 5 seconds for consumer to consume message in another thread.
        verify(inputTopicListenerImpl, timeout(FOUR_SECONDS_IN_MS).times(1))
                .listen(any(), acknowledgmentArgumentCaptor.capture());
        verify(fileProcessorMock, times(1)).processEventFile(any());
        assertThat(acknowledgmentArgumentCaptor.getValue().toString()).contains("testNode0");
        assertThat(acknowledgmentArgumentCaptor.getValue().toString()).contains("offset = 0");
        LOG.info(acknowledgmentArgumentCaptor.getValue().toString());
    }

    @Test
    @Order(2)
    @DisplayName("2 Should run the test and ignore first message and acknowledge second one so listen and mock should be called twice")
    public void the_ignored_file_not_processed() throws IOException, InterruptedException {

        doNothing().doThrow(IOException.class).when(fileProcessorMock).processEventFile(any());

        sendToInputTopic(createProducerRecord(jsonInputTopicMessage1));
        sendToInputTopic(createProducerRecord(jsonInputTopicMessage2));

        TimeUnit.SECONDS.sleep(10);

        verify(inputTopicListenerImpl, timeout(FOUR_SECONDS_IN_MS).times(2))
                .listen(any(), acknowledgmentArgumentCaptor.capture());
        verify(fileProcessorMock, atLeast(2)).processEventFile(any());
        assertThat(acknowledgmentArgumentCaptor.getValue().toString()).contains("testNode2");
        //offset should not be three as message 2 is skipped due to no acknowledgement
        assertThat(acknowledgmentArgumentCaptor.getValue().toString()).contains("offset = 2");
        LOG.info(acknowledgmentArgumentCaptor.getValue().toString());
    }

    @AfterAll
    void tearDownClass() {
        producer.close();
        toggleListeners(false);
    }

    private void sendToInputTopic(ProducerRecord<String, String> producerRecord) {
        producer.send(producerRecord);
        producer.flush();
    }

    private ProducerRecord<String, String> createProducerRecord(final String jsonInputTopicPayload) {
        return new ProducerRecord<>(topicName, "", jsonInputTopicPayload);
    }

    private void createProducer() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producer = new DefaultKafkaProducerFactory<>(configs, new StringSerializer(), new StringSerializer()).createProducer();
    }

    private void toggleListeners(final boolean start) {
        Collection<MessageListenerContainer> messageListenerContainers = registry.getAllListenerContainers();
        LOG.info("Toggle Listeners: " + messageListenerContainers.size());

        for (MessageListenerContainer messageListenerContainer : messageListenerContainers) {
            if (start) {
                if (!messageListenerContainer.isRunning()) {
                    messageListenerContainer.start();
                    // Will break if we add new listeners or partition assignment changes
                    ContainerTestUtils.waitForAssignment(messageListenerContainer, 2);
                }
            } else {
                messageListenerContainer.stop();
            }
        }
    }
}
