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

package com.ericsson.oss.services.sonom.edc.test.steps;

import com.ericsson.oss.services.sonom.common.kafka.exception.KafkaConsumerInstantiationException;
import com.ericsson.oss.services.sonom.edc.test.helpers.HttpRequestHandler;
import com.ericsson.oss.services.sonom.edc.test.helpers.SleepHandler;
import com.ericsson.oss.services.sonom.edc.test.helpers.TestEdcConstants;
import com.ericsson.oss.services.sonom.edc.test.helpers.TestKafkaMessageConsumer;
import com.ericsson.oss.services.sonom.edc.test.helpers.TestKafkaMessageProducer;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * The class EdcDeploymentTest contains the steps needed to test the end to end deployment of Event Data Collector
 */
public class EdcDeploymentTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdcDeploymentTest.class);

    private TestKafkaMessageProducer messageProducer;
    private TestKafkaMessageConsumer messageConsumer;

    @Before
    public void beforeScenario() throws KafkaConsumerInstantiationException {
        messageConsumer = TestKafkaMessageConsumer.getInstance();
        messageProducer = new TestKafkaMessageProducer();
    }

    @After
    public void afterScenario() {
        messageProducer.stop();
        messageConsumer.shutdown();
    }

    @Given("^EDC is fully deployed$")
    public void givenEDCIsDeployed() {
        final int apiHealthCheckResponse = HttpRequestHandler.waitForServiceToBeRunning(TestEdcConstants.EDC_SERVICE,
                TestEdcConstants.EDC_PORT, TestEdcConstants.EDC_HC_ENDPOINT, 10, 30);
        assertThat("Asserting EDC is running", apiHealthCheckResponse, is(200));
    }

    @When("^InputMessages are sent to EDC Deployment$")
    public void whenInputMessagesAreSent() {
        try {
            SleepHandler.sleep(60, "input topic subscription");
            LOGGER.info("Sending InputMessages to '{}'", TestEdcConstants.INPUT_KAFKA_TOPIC);
            messageProducer.sendMessage(TestEdcConstants.TEST_CUCP_INPUT_MESSAGE);
            messageProducer.sendMessage(TestEdcConstants.TEST_DU_INPUT_MESSAGE);
            SleepHandler.sleep(100, "InputMessage processing");
        } catch (final Exception e) {
            LOGGER.error("Error sending InputMessage to EDC", e);
        }
    }

    @Then("Expect {int} Events on Output Topic")
    public void expectEventsOnTheOutputTopic(final int expectedNumberOfEvents) {
        LOGGER.info("Retrieving OutputMessage from '{}'", TestEdcConstants.OUTPUT_KAFKA_TOPIC);
        assertThat("Asserting there is " + expectedNumberOfEvents + " events on the output topic",
                messageConsumer.getNumberOfReceivedEvents(), is(expectedNumberOfEvents));
    }
}
