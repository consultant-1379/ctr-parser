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

package com.ericsson.oss.adc.config;

import com.ericsson.oss.adc.handler.data_catalog.DataCatalogHandler;
import com.ericsson.oss.adc.models.BulkDataRepository;
import com.ericsson.oss.adc.models.DataCollector;
import com.ericsson.oss.adc.models.DataProviderType;
import com.ericsson.oss.adc.models.DataSpace;
import com.ericsson.oss.adc.models.FileFormat;
import com.ericsson.oss.adc.models.FileFormatList;
import com.ericsson.oss.adc.models.MessageBus;
import com.ericsson.oss.adc.models.NotificationTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@EmbeddedKafka
//Set active profile to unique id to ensure full method runs in tests
@ActiveProfiles(profiles = "KafkaTest")
class KafkaConfigurationTest {

    @Autowired
    private KafkaConfiguration kafkaConfiguration;

    @MockBean
    private DataCatalogHandler dataCatalogHandler;

    private FileFormat fileFormat;

    @BeforeEach
    void setupSampleFileFormat(){
        fileFormat = new FileFormat(
                1L,
                new BulkDataRepository(
                        1L,
                        "testBDR",
                        "testCluster",
                        "testNS",
                        new ArrayList<String>(Collections.singletonList("http://endpoint1:1234/")),
                        new ArrayList<Long>(Collections.singletonList(1L))
                ),
                new DataCollector(
                        1L,
                        "DataCollector",
                        "http://controlEndpoint:1234/"
                ),
                new DataProviderType(
                        1L,
                        new DataSpace(
                                1L,
                                "name",
                                new ArrayList<Long>(Arrays.asList(1L, 2L, 3L, 4L, 5L))
                        ),
                        new ArrayList<Long>(Collections.singletonList(1L)),
                        "V1",
                        "CM_NOTIFICATIONS",
                        "v1"
                ),
                new NotificationTopic(
                        1L,
                        "NotificationTopic1",
                        "NotificationTopicSpecRef",
                        "JSON",
                        new ArrayList<Long>(Collections.singletonList(1L)),
                        new MessageBus(
                                1L,
                                "name",
                                "clusterName",
                                "nameSpace",
                                new ArrayList<String>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092", "http://localhost:9092")),
                                new ArrayList<Long>(Collections.singletonList(1L))
                        )
                ),
                new ArrayList<Long>(Collections.singletonList(15L)),
                "XML",
                ""
        );
    }
    @Test
    @DisplayName("Should return comma separated string of kafka access points after querying DataCatalog")
    void test_getKafkaAccessPoints_Success(){
        FileFormatList list = new FileFormatList();
        list.add(fileFormat);
        String expectedEndPoint = "http://endpoint1:1234/,eric-oss-dmm-data-message-bus-kf-client:9092,http://localhost:9092";

        when(dataCatalogHandler.getFileFormatListByDataProviderTypeAndDataSpace(any(), any(), any())).thenReturn(list);
        String actualEndPoint = kafkaConfiguration.getKafkaAccessPoints();

        assertEquals(expectedEndPoint, actualEndPoint);
    }

    @Test
    @DisplayName("Should catch Exception and return empty string as kafka access point")
    void test_getKafkaAccessPoints_Fails(){
        when(dataCatalogHandler.getFileFormatListByDataProviderTypeAndDataSpace(any(), any(), any())).thenThrow(RuntimeException.class);
        String result = assertDoesNotThrow(() -> kafkaConfiguration.getKafkaAccessPoints());
        assertEquals("", result);
    }
}