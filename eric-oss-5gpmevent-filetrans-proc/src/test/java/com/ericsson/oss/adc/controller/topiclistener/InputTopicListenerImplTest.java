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

import com.ericsson.oss.adc.file_processor.FileProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.Acknowledgment;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {InputTopicListenerImpl.class, InputTopicListenerImplTest.class})
public class InputTopicListenerImplTest {

    @MockBean
    FileProcessor fileProcessorMock;

    @Autowired
    InputTopicListenerImpl inputTopicListener;

    @Value("${spring.kafka.topics.input.name}")
    String topicName;

    @Test
    @DisplayName("Should map the json correctly to the InputMessage model and call the processEventFile method once")
    public void test_input_topic_listener_parses_data_correctly() throws Exception {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nodeName", "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=ERBS-SUBNW-1,MeContext=ieatnetsimv6034-10_LTE35ERBS00104");
        jsonObject.put("fileLocation",
                "/ericsson/pmic2/XML/SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=ERBS-SUBNW-1,MeContext=ieatnetsimv6034-10_LTE35ERBS00104/");

        final AtomicBoolean ack = new AtomicBoolean(false);
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>(topicName, 0, 0, "", jsonObject.toString());
        Acknowledgment acknowledgment = () -> ack.set(true);

        doNothing().when(fileProcessorMock).processEventFile(any());

        assertDoesNotThrow(() -> inputTopicListener.listen(consumerRecord, acknowledgment));
        verify(fileProcessorMock, times(1)).processEventFile(any());
        assertTrue(ack.get());
    }

    @Test
    @DisplayName("Should not throw an exception when unexpected data is received and should call the processEventFile method once")
    public void test_input_topic_listener_parses_data_correctly_with_additional_unexpected_data() throws Exception {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nodeName", "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=ERBS-SUBNW-1,MeContext=ieatnetsimv6034-10_LTE35ERBS00104");
        jsonObject.put("fileLocation",
                "/ericsson/pmic2/XML/SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=ERBS-SUBNW-1,MeContext=ieatnetsimv6034-10_LTE35ERBS00104/");
        jsonObject.put("unexpected", "unexpected");

        final AtomicBoolean ack = new AtomicBoolean(false);
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>(topicName, 0, 0, "", jsonObject.toString());
        Acknowledgment acknowledgment = () -> ack.set(true);

        doNothing().when(fileProcessorMock).processEventFile(any());

        assertDoesNotThrow(() -> inputTopicListener.listen(consumerRecord, acknowledgment));
        verify(fileProcessorMock, times(1)).processEventFile(any());
        assertTrue(ack.get());
    }
}

