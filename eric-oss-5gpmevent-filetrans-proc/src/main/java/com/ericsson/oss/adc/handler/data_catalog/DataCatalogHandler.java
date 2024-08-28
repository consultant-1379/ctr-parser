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

package com.ericsson.oss.adc.handler.data_catalog;

import com.ericsson.oss.adc.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;

import static java.util.stream.Collectors.toCollection;

/**
 * Implementation of a handler to communicate with the Data Management and Movement (DM&M) Data Catalog service.
 * This includes different ways of getting the notification topic entity from the service, including by
 * id, name, name and message bus ID, and simply getting all notification topics stored on the data catalog service.
 */
@Component
public class DataCatalogHandler {

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogBaseUrl;

    @Value("${dmm.data-catalog.base-port}")
    private String dataCatalogBasePort;

    @Value("${dmm.data-catalog.notification-topic-uri}")
    private String notificationTopicUri;

    @Value("${dmm.data-catalog.file-format-uri}")
    private String fileFormatUri;

    @Autowired
    private RestTemplate restTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(DataCatalogHandler.class);


    /**
     * Build generic request body for REST calls.
     */
    public HttpEntity<Object> requestStructure(){
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    /**
     * Gets all the file format topics from the file format entity stored on data catalog
     *
     * @return A list of the retrieved FileFormat objects
     */
    public FileFormatList getAllFileFormat() {
        final String url = MessageFormat.format("{0}{1}{2}", dataCatalogBaseUrl, dataCatalogBasePort, fileFormatUri);
        LOG.info("GET all file formats request: {}", url);
        return restTemplate.exchange(url, HttpMethod.GET, requestStructure(), FileFormatList.class).getBody();
    }

    /**
     * Gets all the file format topics from the file format entity stored on data catalog
     * by its dataProvider, data space and data category.
     *
     * @param dataProviderType dataProvider type eg. enm
     * @param dataSpace the data space type eg. 4G, 5G
     * @param dataCategory the data category eg. PM_STATS
     * @return A list of the retrieved FileFormat objects based on the dataProviderType, dataSpace and dataCategory
     */
    public FileFormatList getFileFormatListByDataProviderTypeAndDataSpace(final String dataProviderType, final String dataSpace, final String dataCategory) {
        final String url = MessageFormat.format("{0}{1}{2}?dataProviderType={3}&dataSpace={4}&dataCategory={5}",
                dataCatalogBaseUrl, dataCatalogBasePort, fileFormatUri, dataProviderType, dataSpace, dataCategory);
        LOG.info("GET file format by data provider type and data space request: {}", url);
        return restTemplate.exchange(url, HttpMethod.GET, requestStructure(), FileFormatList.class).getBody();
    }

    /**
     * Gets all the notification topics from the notification topic entity stored on data catalog
     *
     * @return A list of the retrieved NotificationTopic objects
     */
    public NotificationTopicList getAllNotificationTopics() {
        String url = MessageFormat.format("{0}{1}{2}", dataCatalogBaseUrl, dataCatalogBasePort, notificationTopicUri);
        LOG.info("GET notification topic list request: {}", url);
        return restTemplate.exchange(url, HttpMethod.GET, requestStructure(), NotificationTopicList.class).getBody();
    }

    /**
     * Gets a notification topic from the notification topic entity stored on data catalog by its ID
     *
     * @param id ID of the notification topic to be retrieved
     * @return The NotificationTopic object based on the id
     */
    public NotificationTopic getNotificationTopicsById(Long id) {
        String url = MessageFormat.format("{0}{1}{2}{3}", dataCatalogBaseUrl, dataCatalogBasePort, notificationTopicUri, id);
        LOG.info("GET notification topic by ID request: {}", url);
        return restTemplate.exchange(url, HttpMethod.GET, requestStructure(), NotificationTopic.class).getBody();
    }

    /**
     * Gets all the notification topics from the notification topic entity stored on data catalog
     * by its name and the associated message bus ID
     *
     * @param name Name of the notification topic to be retrieved
     * @param messageBusId Message Bus ID associated with the notification topic to be retrieved
     * @return A list of the retrieved NotificationTopic objects based on the name and messageBusId
     */
    public NotificationTopicList getNotificationTopicListByNameAndMessageBusId(String name, Long messageBusId) {
        String url = MessageFormat.format("{0}{1}{2}?name={3}&messageBusId={4}",
                dataCatalogBaseUrl, dataCatalogBasePort, notificationTopicUri, name, messageBusId);
        LOG.info("GET notification topic by name and message bus ID request: {}", url);
        return restTemplate.exchange(url, HttpMethod.GET, requestStructure(), NotificationTopicList.class).getBody();
    }

    /**
     * Gets all the notification topics from the notification topic entity stored on data catalog by its name
     *
     * @param name Name of the notification topic to be retrieved
     * @return A list of the retrieved NotificationTopic objects based on the name
     */
    public NotificationTopicList getNotificationTopicListByName(String name) {
        String url = MessageFormat.format("{0}{1}{2}?name={3}", dataCatalogBaseUrl, dataCatalogBasePort, notificationTopicUri, name);
        LOG.info("GET notification topic list by name request: {}", url);

        return getAllNotificationTopics().stream()
                .filter(notificationTopic ->  name.equals(notificationTopic.getName()))
                .collect(toCollection(NotificationTopicList::new));
    }
}
