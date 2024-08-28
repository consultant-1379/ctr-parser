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

package com.ericsson.oss.adc.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageBus implements Serializable {
    private static final long serialVersionUID = 1;
    private Long id;
    private String name;
    private String clusterName;
    private String nameSpace;
    private ArrayList<String> accessEndpoints;
    private ArrayList<Long> notificationTopicIds;
    private ArrayList<Long> messageStatusTopicIds;
    private ArrayList<Long> messageDataTopicIds;

    public MessageBus(final Long id, final String name, final String clusterName, final String nameSpace, final List<String> accessEndpoints, final List<Long> notificationTopicIds) {
        this.id = id;
        this.name = name;
        this.clusterName = clusterName;
        this.nameSpace = nameSpace;
        this.accessEndpoints = new ArrayList<>(accessEndpoints);
        this.notificationTopicIds = new ArrayList<>(notificationTopicIds);
    }
}
