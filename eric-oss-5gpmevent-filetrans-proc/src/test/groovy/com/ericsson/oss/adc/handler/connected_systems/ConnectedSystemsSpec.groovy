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
package com.ericsson.oss.adc.handler.connected_systems


import org.junit.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("connectedSystemsTest")
@EmbeddedKafka
class ConnectedSystemsSpec extends Specification {

    private static final String URL = "http://eric-eo-subsystem-management/subsystem-manager/v1/subsystems/"

    @Autowired
    private RestTemplate restTemplate

    @Autowired
    private ConnectedSystemsRetriever subsystemRetriever

    private MockRestServiceServer mockServer

    def setup() {
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    def "When connected Systems successful response received, json is parsed and subsystem map is populated"() {
        given: "mocked rest server"
        and: "a mocked get rest response with 4 subsystems"
        expect: "a json response with a list of 4 subsystems"
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new InputStreamResource(getClass().getClassLoader().getResourceAsStream("GetSubsystemsResponse.json"))))
        and: "a populated subsystems map with 4 subsystems "
        Assert.assertEquals(4, subsystemRetriever.getSubsystemDetails().size())
    }

    def "When connected Systems end point returns BAD_REQUEST"() {
        given: "mocked rest server"
        expect: "a json response with no body"
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON))
        and: "subsystems map is not populated"
        Assert.assertEquals(0, subsystemRetriever.getSubsystemDetails().size())
    }

    def "When connected Systems end point returns UNAUTHORIZED"() {
        given: "mocked rest server"
        expect: "a json response with no body"
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON))
        and: "subsystems map is not populated"
        Assert.assertEquals(0, subsystemRetriever.getSubsystemDetails().size())
    }

    def "When connected Systems end point returns FORBIDDEN"() {
        given: "mocked rest server"
        expect: "a json response with no body"
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON))
        and: "subsystems map is not populated"
        Assert.assertEquals(0, subsystemRetriever.getSubsystemDetails().size())
    }

    def "When connected Systems end point returns NOT_FOUND"() {
        given: "mocked rest server"
        expect: "a json response with no body"
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON))
        and: "subsystems map is not populated"
        Assert.assertEquals(0, subsystemRetriever.getSubsystemDetails().size())
    }

    def "When connected Systems end point returns SERVICE_UNAVAILABLE"() {
        given: "mocked rest server"
        expect: "a json response with no body"
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON))
        and: "subsystems map is not populated"
        Assert.assertEquals(0, subsystemRetriever.getSubsystemDetails().size())
    }

    def cleanup() {
        subsystemRetriever.getSubsystemsByNameMap().clear()
    }
}
