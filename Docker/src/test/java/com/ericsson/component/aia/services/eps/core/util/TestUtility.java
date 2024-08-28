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

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.component.aia.model.base.util.DataConverters;

/**
 * Utility class for test.
 */
public final class TestUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtility.class);

    /**
     * Private constructor.
     */
    private TestUtility() {
        super();
    }

    /**
     * @return available port else throw runtime exception.
     */
    public static int findFreePort() {
        int port;
        try {
            final ServerSocket socket = new ServerSocket(0);
            socket.setReuseAddress(true);
            port = socket.getLocalPort();
            socket.close();
        } catch (final Exception e) {
            LOGGER.error("Exception while finding free port ", e);
            throw new RuntimeException("Unable to find free port", e);
        }
        return port;
    }

    /**
     * @return appropriate separator expected by IPL while looking for Local:// .
     */
    public static String getLocalURI() {
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX) {
            return "/";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            return "//";
        }
        return null;
    }

    /**
     *
     * The helper utility for generic writer.
     *
     * @param value
     *            object
     * @param variableName
     *            attribute/filed that needs to extract from object.
     * @param schemaName
     *            name of class/schema.
     * @return attribute value
     */
    @SuppressWarnings("rawtypes")
    public static String convertToString(final Object value, final String variableName, final String schemaName) {

        if (null == value) {
            return null;
        }
        if (value instanceof Integer) {
            return Integer.toString((Integer) value);
        } else if (value instanceof Long) {
            return Long.toString((Long) value);
        } else if (value instanceof Byte) {
            return Byte.toString((Byte) value);
        } else if (value instanceof Boolean) {
            return Boolean.toString((Boolean) value);
        } else if (value instanceof ByteBuffer) {
            return DataConverters.byteArray2String(((ByteBuffer) value).array());
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof byte[]) {
            final byte[] value2 = (byte[]) value;
            return new String(value2);
        } else if (value instanceof Collection) {
            try {
                return convertViaPojoMapping((Collection) value, variableName, schemaName);
            } catch (final ClassNotFoundException e) {
                System.err.println("UNKNOWN===>" + value.getClass());
                return "UNKNOWN class";
            }
        } else {
            System.err.println("UNKNOWN===>" + value.getClass());
            return "UNKNOWN value";
        }
    }

    /**
     * Collection helper method to access the collection object and convert to its equivalent string representation which uses # symbol as separator.
     *
     * @param value2
     *            collection object
     * @param variableName
     *            filed/attribute name
     * @param schemaName
     *            schema/class name.
     * @return string representation of collection.
     * @throws ClassNotFoundException
     */
    private static String convertViaPojoMapping(@SuppressWarnings("rawtypes") Collection value2, final String variableName, final String schemaName)
            throws ClassNotFoundException {
        Class<?> pojoClass;
        try {
            pojoClass = Class.forName("com.ericsson.component.aia.model.generated.eventbean.ebm." + schemaName);
        } catch (final ClassNotFoundException e) {
            pojoClass = Class.forName("com.ericsson.component.aia.model.generated.eventbean.celltrace." + schemaName);
        }

        final Field[] fields = pojoClass.getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {
            final Field f = fields[i];
            if (f.getName().equals(variableName)) {
                if (f.toString().contains("byte[]")) {
                    final Collection<Byte> byteCast = new ArrayList<>();
                    for (int j = 0; j < value2.size(); j++) {

                        int value = (int) value2.toArray()[j];
                        if (value < -127) {
                            value = (value >> 24) & 0xFF;
                        }
                        byteCast.add((byte) value);
                    }
                    value2 = byteCast;
                    break;
                }

            }
        }

        return "[" + StringUtils.join(value2, "#") + "]";
    }

    /**
     * The method will do the deep comparison of two file line by line.<br>
     * It will verify exactly identical files. File with minor space issue will fail the comparison.
     *
     * @param source
     *            source file
     * @param target
     *            target file
     * @return true on if test pass else false.
     */
    public static boolean deepCompare(final File source, final File target) {
        boolean result = true;
        try {
            final Set<String> sourceContents = Files.lines(Paths.get(source.getAbsolutePath())).collect(Collectors.toSet());
            final Set<String> targetContents = Files.lines(Paths.get(target.getAbsolutePath())).collect(Collectors.toSet());

            if (sourceContents.size() != targetContents.size()) {
                result = false;
            }
            sourceContents.removeAll(targetContents);
            if (!sourceContents.isEmpty()) {
                result = false;
            }

        } catch (final IOException e) {
            LOGGER.error("IO Exception while doing deep compare ", e);
            result = false;
        }
        return result;
    }

    /**
     * Query the JMX metrics for specific attribute to get the value associated with it. <br>
     * <UL>
     * <li><b>Name:</b> com.ericsson.component.aia.services.eps.core.statistics.EPS_INSTANCE_ID:name=PMFileParser@erroneousFiles</li>
     * <li><b>Attribute:</b> Count</li>
     * </UL>
     * <br>
     *
     * @param name
     *            name of the metrics
     * @param attributeName
     *            Attribute associated with metrics
     * @return return on success return the value associated with metrics else -1.
     */
    public static long getJMXvalues(final String name, final String attributeName) {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            String expectedObjectName = "com.ericsson.component.aia.services.eps.core.statistics." + "*:name=" + name;
            final Set<ObjectName> searchByObjectNamePattern = server
                    .queryNames(new ObjectName(expectedObjectName), null);
            if (!searchByObjectNamePattern.isEmpty()) {
                final ObjectName next = searchByObjectNamePattern.iterator().next();
                long value = (long) server.getAttribute(next, attributeName);
                LOGGER.info("JMX Query {}, value: {}", next.getCanonicalName(), value);
                return value;
            }

        } catch (final Exception e) {
            LOGGER.error("Error will query to MBean to get the value for " + name, e);
        }

        return 0;
    }

}
