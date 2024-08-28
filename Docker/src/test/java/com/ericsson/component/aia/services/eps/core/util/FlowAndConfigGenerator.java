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

import java.io.*;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FlowXmlGenerator class generates flow.xml based on the passed parameters
 */
public class FlowAndConfigGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowAndConfigGenerator.class);

    /**
     * This method creates flow xml based on passed configurations parameters.
     *
     * @param inputTemplate
     *            the input
     * @param outputFolder
     *            the output folder
     * @param attributeMap
     *            the attribute map
     * @param outputFileName
     *            the file
     * @throws Exception
     *             the exception
     */
    public static File createFlowXmlFile(final String inputTemplate, final File outputFolder, final Map<String, Object> attributeMap,
                                         final String outputFileName) {
        try {
            return writeToFile(inputTemplate, outputFolder, attributeMap, outputFileName);
        } catch (final Exception error) {
            LOGGER.error("Error while generating file {} " + outputFileName, error);
            return null;
        }
    }

    /**
     * This method won't create parent folder if it is not exist.
     *
     * @param input
     *            the input
     * @param outputFolder
     *            the output folder
     * @param attributeMap
     *            the attribute map
     * @param file
     *            the file
     * @throws Exception
     *             the exception
     */
    private static File writeToFile(final String input, final File outputFolder, final Map<String, Object> attributeMap, final String file)
            throws Exception {
        final File outputFile = new File(outputFolder.getAbsolutePath() + File.separator + file);
        outputFile.createNewFile();
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF8"));
        initTemplate(writer, input, attributeMap);
        writer.flush();
        writer.close();
        return outputFile;
    }

    private static void initTemplate(final Writer writer, final String input, final Map<String, Object> attributeMap) throws Exception {
        /* first, get and initialize an engine */

        final VelocityEngine ve = new VelocityEngine();
        final Properties p = new Properties();
        p.setProperty("resource.loader", "class");
        p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

        ve.init(p);

        /* next, get the Template */
        final Template template = ve.getTemplate(input);

        /* create a context and add data */
        final VelocityContext context = new VelocityContext();

        // loop a Map
        for (final Map.Entry<String, Object> entry : attributeMap.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }

        if (template != null) {
            template.merge(context, writer);
        }
    }

}