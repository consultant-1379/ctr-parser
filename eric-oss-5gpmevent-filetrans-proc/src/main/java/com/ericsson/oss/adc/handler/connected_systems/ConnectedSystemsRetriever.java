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
package com.ericsson.oss.adc.handler.connected_systems;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.ericsson.oss.adc.models.connected.systems.SubsystemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ConnectedSystemsRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedSystemsRetriever.class);

    @Value("${connected.systems.url}")
    private String url;

    private final Map<String, SubsystemDTO> subsystemsByNameMap =  new HashMap<>();

    @EventListener(value = ApplicationReadyEvent.class, condition = "@environment.getActiveProfiles()[0] == 'prod'")
    public void getSubsystemDetailsOnStartUp() {
        LOGGER.info("Requesting Subsystems from Connected Systems on Startup {} ", url);
        getSubsystemDetails();
    }

    @Autowired
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, SubsystemDTO> getSubsystemDetails() {
        try {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            final HttpEntity<Object> entity = new HttpEntity<>(headers);
            final ResponseEntity<SubsystemDTO[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, SubsystemDTO[].class);
            return processResponse(response);
        } catch (final Exception exception) {
            LOGGER.error("Failed to execute {} request ", url, exception);
            return Collections.emptyMap();
        }
    }

    private Map<String, SubsystemDTO> populateSubsystemByNameMap(final List<SubsystemDTO> subsystemList) {
        for (final SubsystemDTO subsystem: subsystemList) {
            subsystemsByNameMap.put(subsystem.getName(), subsystem);
        }
        return subsystemsByNameMap;
    }

    private Map<String, SubsystemDTO> processResponse(final ResponseEntity<SubsystemDTO[]> response) {
        final int responseCode = response.getStatusCode().value();
        LOGGER.info("Successfully executed request {}, response {} ", url, responseCode);
        if (responseCode == Response.Status.OK.getStatusCode()) {
            final List<SubsystemDTO> subsystemList =  Arrays.asList(response.getBody());
            return populateSubsystemByNameMap(subsystemList);
        }
        return Collections.emptyMap();
    }

    //Needed for Test
    public Map<String, SubsystemDTO> getSubsystemsByNameMap() {
        return subsystemsByNameMap;
    }
}
