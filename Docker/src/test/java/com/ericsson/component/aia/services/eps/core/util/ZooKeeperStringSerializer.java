/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.component.aia.services.eps.core.util;

import java.nio.charset.Charset;

import org.I0Itec.zkclient.serialize.ZkSerializer;

/**
 * Zookeeper utility expected serialize string based ZooKeeperStringSerializer provide simple implementation of it.
 *
 */
public class ZooKeeperStringSerializer implements ZkSerializer {
    private static final Charset CHARSET = Charset.forName("UTF-8");

    @Override
    public byte[] serialize(final Object data) {
        if (data instanceof String) {
            return ((String) data).getBytes(CHARSET);
        }

        throw new IllegalArgumentException("ZooKeeperStringSerializer can only serialize strings.");
    }

    @Override
    public Object deserialize(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        return new String(bytes, CHARSET);
    }
}
