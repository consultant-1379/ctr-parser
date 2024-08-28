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
package com.ericsson.oss.adc.models.connected.systems;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SubsystemDTO {

    private Long id;
    private Long subsystemTypeId;
    private String name;
    private String url;
    private String healthCheckTime;
    private List<ConnectionPropertiesDTO> connectionProperties;
    private String vendor;
    private SubsystemTypeDTO subsystemType;
    private String adapterLink;

    public void setConnectionProperties(final List<ConnectionPropertiesDTO> connectionProperties) {
        this.connectionProperties = new ArrayList<>(connectionProperties);
    }

    public void setSubsystemType(final SubsystemTypeDTO subsystemType) {
        this.subsystemType = subsystemType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public void setSubsystemTypeId(final Long subsystemTypeId) {
        this.subsystemTypeId = subsystemTypeId;
    }

    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    public void setAdapterLink(final String adapterLink) {
        this.adapterLink = adapterLink;
    }

}
