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
import java.nio.Buffer;
import java.util.*;

import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@linkplain GenericRecordCsvWriter} generate the separate files for each of the event.
 */
public class GenericRecordCsvWriter {
    private final static Logger LOG = LoggerFactory.getLogger(GenericRecordCsvWriter.class);

    private List<String> header = new LinkedList<String>();
    private boolean printHeader = true;
    private BufferedWriter writer;
    File tempDir;
    private String filename;

    /**
     * The constructor to create instance of writer.
     *
     * @param fileName
     *            Name of file
     * @param outputDir
     * @param enableShutdownHook
     */
    public GenericRecordCsvWriter(final String fileName, final File outputDir, final boolean enableShutdownHook) {
        this.filename = fileName;
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        try {
            writer = new BufferedWriter(new FileWriter(new File(outputDir.getAbsolutePath() + File.separator + fileName + ".csv")));
        } catch (final IOException e) {
            LOG.error("Error while creating file {}", outputDir.getAbsolutePath() + File.separator + fileName, e);
        }

        LOG.info("Shutdown hook is enabled {}", enableShutdownHook);

        if (enableShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        writer.flush();
                        writer.close();
                        LOG.info("ShutdownHook cleaning  {}", fileName);
                    } catch (final IOException e) {
                        LOG.error("ShutdownHook cleaning  {}", fileName, e);
                    }
                }
            });
        }
    }

    /**
     * Write Generic records to file.
     *
     * @param record
     *            generic record.
     * @throws IOException
     */
    public void write(final GenericRecord record) throws IOException {

        final StringBuilder builder = new StringBuilder();
        if (printHeader) {
            final List<Field> fields = record.getSchema().getFields();
            for (final Field f : fields) {
                builder.append(f.name());
                builder.append(",");
                header.add(f.name());
            }
            builder.setLength(builder.length() - 1);
            writer.write(builder.toString() + "\n");
            printHeader = false;
            builder.setLength(0);
        }

        for (final String str : header) {
            final Object paramObject = record.get(str);
            if (paramObject instanceof Buffer || paramObject instanceof Collection) {
                builder.append(TestUtility.convertToString(paramObject, str, record.getSchema().getName()));
            } else {
                builder.append(paramObject);
            }
            builder.append(",");
        }
        builder.setLength(builder.length() - 1);
        writer.write(builder.toString() + "\n");
        builder.setLength(0);
        writer.flush();
    }

    /**
     * Close the generic record writer.
     */
    public void close() {
        try {
            writer.flush();
            writer.close();
            LOG.info("{} closed successfully", filename);
        } catch (final IOException e) {
            LOG.error("Erro while closeing  {}", filename, e);
        }
    }
}
