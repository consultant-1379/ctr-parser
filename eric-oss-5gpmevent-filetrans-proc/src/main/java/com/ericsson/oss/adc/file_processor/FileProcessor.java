/*******************************************************************************
 * COPYRIGHT Ericsson 2021 - 2022
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

package com.ericsson.oss.adc.file_processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.oss.adc.handler.output_topic.OutputTopicHandler;
import com.ericsson.oss.adc.models.DecodedEvent;
import com.ericsson.oss.adc.models.InputMessage;
import com.ericsson.oss.adc.models.Metrics;
import com.ericsson.pm_event.PmEventOuterClass;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Implementation for processing the 5G event files. This involves reading an event file from ephemeral storage, decompressing it, breaking the events
 * up into records and writing the records to Kafka.
 */
@Component
public class FileProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FileProcessor.class);
    private static final int BUFFER_SIZE = 65536;
    private static final long EVENT_ID_2004 = 2004;
    private static final long EVENT_ID_3054 = 3054;
    private static final long EVENT_ID_2008 = 2008;
    private final Counter numEventFilesProcessed;
    private final AtomicLong processedFileDataVolume;

    @Autowired
    private OutputTopicHandler outputTopicHandler;

    @Value("${temp-directory}")
    private String outputPathPrefix;

    public FileProcessor(final MeterRegistry meterRegistry) {
        numEventFilesProcessed = meterRegistry.counter("eric.oss.5gpmevent.filetrans.proc:event.files.processed");
        processedFileDataVolume = meterRegistry.gauge("eric.oss.5gpmevent.filetrans.proc:processed.file.data.volume", new AtomicLong(0));
    }

    /**
     * Process the event file received from the input topic. Transactional. If processing of file fails, exception will be thrown and transaction will
     * be aborted.
     *
     * @param inputMessage
     *            input message received from the input file notification topic
     */
    public void processEventFile(final InputMessage inputMessage) throws IOException {
        String fileLocation = null;
        try {
            fileLocation = decompressEventFile(inputMessage.getPath(), outputPathPrefix);
            final Metrics metrics = splitEvents(fileLocation, inputMessage.getPath());
            numEventFilesProcessed.increment();

            LOG.info("InputMessage successfully processed: {} - {}", inputMessage, metrics);
        } catch (final IOException e) {
            throw new IOException("Error whilst processing event file:" + inputMessage, e);
        } finally {
            LOG.info("Cleaning up files now: [{}]  [{}]", inputMessage.getPath(), fileLocation);
            cleanUp(inputMessage.getPath());
            cleanUp(fileLocation);
        }
    }

    /**
     * Decompress the 5G event file and write to ephemeral storage on the pod
     *
     * @param inputFilePath
     *            path to the compressed file to be decompressed
     * @param outputPathPrefix
     *            the beginning of output path to the decompressed file
     * @return String representing full path to decompressed file
     */
    protected String decompressEventFile(final String inputFilePath, final String outputPathPrefix) throws IOException {
        final String fileName = new File(inputFilePath).getName();
        final String hash = String.valueOf(String.valueOf(System.currentTimeMillis()).hashCode());
        if (!fileName.substring(fileName.lastIndexOf('.') + 1).equalsIgnoreCase("gz")) {
            LOG.info("File already decompressed: {}", inputFilePath);
            return inputFilePath;
        }
        final String decompressedFilePath = outputPathPrefix + fileName.substring(0, fileName.lastIndexOf('.')) + "-" + hash;
        final byte[] buffer = new byte[BUFFER_SIZE];
        try (final FileInputStream fileInputStream = new FileInputStream(inputFilePath);
                final GZIPInputStream gZipInputStream = new GZIPInputStream(fileInputStream, BUFFER_SIZE);
                final FileOutputStream fileOutputStream = new FileOutputStream(decompressedFilePath)) {
            int bytesRead;
            while ((bytesRead = gZipInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        }
        LOG.info("File decompressed: {}", decompressedFilePath);

        return decompressedFilePath;
    }

    /**
     * Split the 5g event file into individual events
     *
     * @param fileLocation
     *            location of the PM event file to be processed
     * @param nodeName
     *            string name of ENM Node from which the event file came
     * @return Metrics of total records read and total records sent
     */
    protected Metrics splitEvents(final String fileLocation, final String nodeName) throws IOException {
        final Metrics metrics;
        long maxRecordLength = 0;
        long sumRecordLength = 0;
        long sentRecordCount = 0;
        long totalRecordCount = 0;
        final List<Long> eventIdList = Arrays.asList(EVENT_ID_2004, EVENT_ID_3054, EVENT_ID_2008);

        final Instant start = Instant.now();
        try (final RandomAccessFile randomAccessFile = new RandomAccessFile(fileLocation, "r");
                final FileChannel fileChannel = randomAccessFile.getChannel()) {
            final MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            final CodedInputStream eventStream = CodedInputStream.newInstance(mappedByteBuffer);
            while (!eventStream.isAtEnd()) {
                final int recordLength = eventStream.readRawVarint32();
                final int limit = eventStream.pushLimit(recordLength);
                final Parser<PmEventOuterClass.PmEvent> parser = PmEventOuterClass.PmEvent.parser();
                final PmEventOuterClass.PmEvent pmEvent = parser.parseFrom(eventStream);

                if (eventIdList.contains(pmEvent.getEventId())) {
                    final int msgLengthSize = eventStream.getTotalBytesRead();
                    LOG.debug("Message Length = {}, Push limit = {}, Message Length Size = {}, Event ID: {}, Node Name = {}",
                            recordLength, limit, msgLengthSize, pmEvent.getEventId(), nodeName);
                    sumRecordLength += recordLength;
                    if (recordLength > maxRecordLength) {
                        maxRecordLength = recordLength;
                    }
                    LOG.debug("Max record length: {}, Sum record length: {}", maxRecordLength, sumRecordLength);

                    outputTopicHandler.sendKafkaMessage(pmEvent, nodeName);
                } else {
                    LOG.debug("Event ID '{}' not found", pmEvent.getEventId());
                }

                eventStream.popLimit(limit);
                eventStream.resetSizeCounter();
                totalRecordCount++;
                sentRecordCount++;
            }
        } catch (final IOException e) {
            processedFileDataVolume.set(0);
            LOG.error("Error splitting events: {}", e.getMessage());
            throw e;
        }
        metrics = new Metrics(sentRecordCount, totalRecordCount);
        LOG.info("Processing file took in milli: {}", Duration.between(start, Instant.now()).toMillis());
        return metrics;
    }

    /**
     * Decodes the body of individual events to retrieve the event's payload.
     *
     * @param eventStream
     *            stream of the current Event to be decoded
     * @return the decoded event
     */
    protected DecodedEvent decodeEvent(final CodedInputStream eventStream) throws InvalidProtocolBufferException {
        final PmEventOuterClass.PmEvent message = PmEventOuterClass.PmEvent.parser().parseFrom(eventStream);
        final String computeName = message.getHeader().getComputeName();
        final String networkManagedElement = message.getHeader().getNetworkManagedElement();
        final String eventID = Long.toString(message.getEventId());
        final long pmEventGroupVersion = message.getPmEventGroupVersion();
        final long pmEventCommonVersion = message.getPmEventCommonVersion();
        final long pmEventCorrectionVersion = message.getPmEventCorrectionVersion();
        final ByteString payload = message.getPayload();

        return new DecodedEvent(pmEventGroupVersion, pmEventCommonVersion,
                pmEventCorrectionVersion, eventID, computeName, networkManagedElement, payload.toByteArray());
    }

    /**
     * Perform clean up after processing the 5G event file - removing temporary files from ephemeral storage
     *
     * @param fileToDelete
     *            the path to the file to be deleted
     */
    protected void cleanUp(final String fileToDelete) {
        if (fileToDelete != null) {
            try {
                LOG.info("Deleting {}", fileToDelete);
                final Path filePath = new File(fileToDelete).toPath();
                Files.delete(filePath);
            } catch (final IOException e) {
                LOG.error("Error Attempting to delete file:", e);
            }
        } else {
            LOG.error("File path provided for deletion is null");
        }
    }

}
