/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.component.aia.services.eps.bit.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.component.aia.services.eps.core.util.*;

import joptsimple.internal.Strings;

/**
 * The {@linkplain BaseTest} provide the helper methods to facilitate common task require for every test.
 */
public abstract class BaseTest {

    private final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);

    //empty event filter list JSON response
    protected static final String EMPTY_FILTERLIST = "{\"data\": {\"events\": []}, \"name\": \"eventlist\", \"title\": \"parser supported events\"}";

    //JSON response with two event ids of events: UE_MEAS_INTRAFREQ1, INTERNAL_PER_RADIO_UE_MEASUREMENT_TA
    protected static final String FILTERLIST = "{\"data\": {\"events\": [5153, 3108]}, \"name\": \"eventlist\", \"title\": \"parser supported events\"}";
    protected static final String EVENTLIST_URL = "/cm/api/v1/configurations/eventlist";
    protected static final int WIREMOCK_PORT = 5003;
    protected String inputTopic = "epsFileInput";
    protected String outputTopic = "epsOut";
    protected String kafkaBrokerList = "localhost:9092";
    protected String filterEventIds = Strings.EMPTY;
    protected String randomToken = "007";


    public File createFlowAndConfigFiles(final File createTempDir) {
        randomToken = String.valueOf(((int) (Math.random() * 1000 + 10)));
        LOGGER.debug("Random token created  {} ", randomToken);
        inputTopic = "epsFileInput" + randomToken;
        outputTopic = "epsOut" + randomToken;
        LOGGER.debug("Kafka input topic [{}] and output topic [{}] ", inputTopic, outputTopic);
        LOGGER.info("Test Directory {} ", createTempDir.getAbsolutePath());
        final String dynamicFlowName = "esonFlow_bit_test" + randomToken + ".xml";
        final Map<String, List<Map<String, String>>> context = new LinkedHashMap<>();
        context.put("ipAdatperlist", populateDataSourceConfig(createTempDir));
        context.put("attributeStepMap", populateFlowStepsConfig(createTempDir));
        context.put("opAdatperlist", populateDataSinkConfig(createTempDir));
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final File createFlowXmlFile = FlowAndConfigGenerator.createFlowXmlFile(Constants.FLOW_VELOCITY_TEMPLATE, createTempDir, (Map) context,
                dynamicFlowName);
        LOGGER.info("Flow File {} create successfully {} ", createFlowXmlFile.getAbsolutePath(), createFlowXmlFile.exists());
        return createFlowXmlFile;
    }

    /**
     * @param createTempDir
     *            temporary dir where config file needs to create.
     * @return config map.
     */
    private List<Map<String, String>> populateDataSinkConfig(final File createTempDir) {
        final Map<String, String> opAdapter = new LinkedHashMap<String, String>();
        final List<Map<String, String>> opAdatperlist = new ArrayList<>();
        opAdatperlist.add(opAdapter);
        final String publisherIPL = "publisherIPL" + randomToken + "_INTEGRATION_POINT.json";
        opAdapter.put("name", "kafkaOutputAdapter");
        opAdapter.put("uri", "avro:/");
        opAdapter.put("integration.point.uri", "local:/" + TestUtility.getLocalURI() + createTempDir.getAbsolutePath());
        opAdapter.put("integration.point.name", publisherIPL.substring(0, publisherIPL.indexOf(".json")));

        final Map<String, String> outputtIPL = new HashMap<>();
        outputtIPL.put("bootstrapServers", kafkaBrokerList);
        outputtIPL.put("topicName", outputTopic);
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final File publisheriplFile = FlowAndConfigGenerator.createFlowXmlFile(Constants.IPL_PUBLISHER_VELOCITY_TEMPLATE, createTempDir,
                (Map) outputtIPL, publisherIPL);

        LOGGER.info("Publisher IPL File {} create successfully {} ", publisheriplFile.getAbsolutePath(), publisheriplFile.exists());

        return opAdatperlist;
    }

    /**
     * @param createTempDir
     *            temporary dir where config file needs to create.
     * @return config map.
     */
    private List<Map<String, String>> populateDataSourceConfig(final File createTempDir) {
        final List<Map<String, String>> ipAdatperlist = new ArrayList<>();
        final Map<String, String> ipAdapter = new LinkedHashMap<String, String>();
        ipAdatperlist.add(ipAdapter);
        final String subscriberIPL = "subscriberIPL" + randomToken + "_INTEGRATION_POINT.json";
        ipAdapter.put("name", "kafkaInputAdaptor");
        ipAdapter.put("uri", "generic:/");
        ipAdapter.put("integration.point.uri", "local:/" + TestUtility.getLocalURI() + createTempDir.getAbsolutePath());
        ipAdapter.put("integration.point.name", subscriberIPL.substring(0, subscriberIPL.indexOf(".json")));

        final Map<String, String> inputIPL = new HashMap<>();
        inputIPL.put("bootstrapServers", kafkaBrokerList);
        inputIPL.put("topicName", inputTopic);
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final File subscriberIPLFile = FlowAndConfigGenerator.createFlowXmlFile(Constants.IPL_SUBSCRIBER_VELOCITY_TEMPLATE, createTempDir,
                (Map) inputIPL, subscriberIPL);

        LOGGER.info("Subscriber IPL File {} create successfully {} ", subscriberIPLFile.getAbsolutePath(), subscriberIPLFile.exists());
        return ipAdatperlist;
    }

    /**
     * @param createTempDir
     *            temporary dir where config file needs to create.
     * @return config map.
     */
    private List<Map<String, String>> populateFlowStepsConfig(final File createTempDir) {
        final String eventFilterListFileName = "eventfilter" + randomToken + ".json";
        final List<Map<String, String>> steplist = new ArrayList<>();
        final Map<String, String> stepMap = new LinkedHashMap<String, String>();
        steplist.add(stepMap);
        stepMap.put("handler", "com.ericsson.component.aia.services.exteps.eh.parser.PMFileParser");
        stepMap.put("schematype", "celltrace");
        stepMap.put("decodedEventType", "generic_record");
        stepMap.put("eventFilterPath", "http://localhost:" + WIREMOCK_PORT + EVENTLIST_URL);

        final Map<String, String> filteredEventList = new HashMap<>();
        filteredEventList.put("eventlist", filterEventIds);

        final File eventFilterFile = FlowAndConfigGenerator.createFlowXmlFile(Constants.EVENT_FILTER_VELOCITY_TEMPLATE, createTempDir,
                (Map) filteredEventList, eventFilterListFileName);
        LOGGER.info("Event filter File {} create successfully {} ", eventFilterFile.getAbsolutePath(), eventFilterFile.exists());
        return steplist;
    }

    /**
     * Publish string data and wait for the response.
     */
    protected void publishFileLocationToKafkaTopic(final String topicName, final String filePath) {
        final KafkaProducer<String, String> producer = new KafkaProducer<String, String>(producerConfig());
        final String replaceAll = filePath.replaceAll("\\\\", "~").replaceAll("~", "\\\\\\\\");
        final String message = "{\"path\":\"" + replaceAll + "\"}";
        LOGGER.info("Publishing {} to kafkatopic {}", message, topicName);
        producer.send(new ProducerRecord<String, String>(topicName, 0, "1", message));
        producer.flush();
        producer.close();
    }

    /**
     * Populate producer related configuration.
     *
     * @return producer configuration.
     */
    private Properties producerConfig() {
        final Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", kafkaBrokerList);
        producerProps.put("acks", "all");
        producerProps.put("retries", 100);
        producerProps.put("batch.size", 1);
        producerProps.put("linger.ms", 1);
        producerProps.put("buffer.memory", 1024);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return producerProps;
    }

    /**
     * The method will do the deep comparison of two file line by line.<br>
     * It will verify exactly identical files. File with minor space issue will fail the comparison.
     *
     * @param sourceFolder
     * @param sourceFileExtension
     * @param targetFolder
     * @param targetFileExtension
     * @param interestedEvents
     * @return
     */
    protected boolean compareResult(final File sourceFolder, final String sourceFileExtension, final File targetFolder,
                                    final String targetFileExtension, final String... interestedEvents) {
        boolean result = true;
        try {

            Map<String, File> sourceFiles = Files.list(Paths.get(sourceFolder.getAbsolutePath())).map(a -> a.toFile())
                    .filter(fileName -> fileName.getName().endsWith(sourceFileExtension) && isInterested(fileName.getName(), interestedEvents)).collect(
                            Collectors.toMap(fileName -> fileName.getName().substring(0, fileName.getName().indexOf(sourceFileExtension)), s1 -> s1));

            LOGGER.info("source files: {}", sourceFiles);

            final Map<String, File> targetFiles = Files.list(Paths.get(targetFolder.getAbsolutePath())).map(a -> a.toFile())
                    .filter(fileName -> fileName.getName().endsWith(targetFileExtension)).collect(
                            Collectors.toMap(fileName -> fileName.getName().substring(0, fileName.getName().indexOf(targetFileExtension)), s1 -> s1));

            LOGGER.info("Target files: {}", targetFiles);

            if (sourceFiles.size() != targetFiles.size() || !sourceFiles.keySet().containsAll(targetFiles.keySet())) {
                result = false;
                Assert.fail(String.format("Source %s and target  %s  are not same", sourceFiles.keySet(), targetFiles.keySet()));
            }

            for (final String key : sourceFiles.keySet()) {
                final boolean deepCompare = TestUtility.deepCompare(sourceFiles.get(key), targetFiles.get(key));
                if (!deepCompare) {
                    result = false;
                    Assert.fail(String.format("Content Validation failed for Source %s and target  %s  file", sourceFiles.get(key),
                            targetFiles.get(key)));
                }
            }

        } catch (final IOException e) {
            result = false;
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }

        return result;

    }

    /**
     * This method is used to check whether the filename comes from events we are interested.
     *
     * If the events of interested are empty or null, we will allow all the filenames. Or else, only filename
     * contains interested events will be allowed.
     *
     * @param key filename containing the event name.
     * @param interestedEvents array of interested events.
     * @return
     */
    protected boolean isInterested(String key, String[] interestedEvents){
        if ( interestedEvents == null || interestedEvents.length == 0){
            LOGGER.info("Empty interested event list, allow all events");
            return true;
        }
        for (String interestedEvent: interestedEvents){
            if ( key.contains(interestedEvent)){
                return true;
            }
        }
        return false;
    }
}