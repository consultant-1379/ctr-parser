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
package com.ericsson.component.aia.services.eps.core.embeddedservices;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.component.aia.services.eps.core.util.GenericRecordCsvWriter;

/**
 * The {@linkplain KafkaGenericRecordConsumer} provides classic consumer implementation to consume messages from KAFKA topic and write to csv file.
 * <br>
 * Note: AIA Transport API provides Kafka implementation, but when we run the managed Kafka subscriber to consume kafka messages, it utilized system
 * resources which is high for embedded services to run all the things together like Kafka, zookeeper, eps, publisher, subscriber parser etc... Hence
 * writing separate consumer to simplify this process.
 *
 */
public class KafkaGenericRecordConsumer {

    private final static Logger LOG = LoggerFactory.getLogger(KafkaGenericRecordConsumer.class);
    private Properties kafkaProperties = new Properties();
    private boolean isRunning = false;
    private File outputDir;
    private Thread poolingThread;
    private Thread consumerThread;
    private QueueConsumer sharedQueue;
    private String kafkabrokers;
    private String zookeeperUrl;
    private String topic;
    private String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
    private String valueDeserializer = "com.ericsson.component.aia.common.avro.kafka.decoder.KafkaGenericRecordDecoder";

    /**
     * Default constructor.
     *
     * @param outputDir
     *            output dir where consume record needs to write.
     * @param topic
     *            name of topic from which data needs to consume.
     * @param kafkabrokers
     *            kafka broker url.
     * @param zookeeperUrl
     *            the url of zookeeper
     */
    public KafkaGenericRecordConsumer(final File outputDir, final String topic, final String kafkabrokers, final String zookeeperUrl) {
        this.outputDir = outputDir;
        this.topic = topic;
        this.kafkabrokers = kafkabrokers;
        this.zookeeperUrl = zookeeperUrl;
    }

    /**
     * @return consume record count.
     */
    public int getConsumeCount() {
        return sharedQueue != null ? sharedQueue.getConsumeCount() : 0;
    }

    /**
     * Helper method to populate the consumer configuration property.
     */
    private void populateConsumerConfig() {
        kafkaProperties.put("bootstrap.servers", kafkabrokers);
        kafkaProperties.put("group.id", "kafka_output_consumer" + ((int) Math.random() * 100 + 5));
        kafkaProperties.put("enable.auto.commit", "true");
        kafkaProperties.put("auto.commit.interval.ms", "100");
        kafkaProperties.put("session.timeout.ms", "30000");
        kafkaProperties.put("auto.offset.reset", "earliest");
        kafkaProperties.put("key.deserializer", keyDeserializer);
        kafkaProperties.put("value.deserializer", valueDeserializer);
        kafkaProperties.put("offsets.topic.replication.factor", "1");
        kafkaProperties.put("partition", "0");
        kafkaProperties.put("zookeeper", zookeeperUrl);
    }

    public void start() {
        if (!isRunning) {
            populateConsumerConfig();
            isRunning = true;
            final BlockingQueue<GenericRecord> queue = new LinkedBlockingDeque<GenericRecord>();
            sharedQueue = new QueueConsumer(queue, outputDir);
            consumerThread = new Thread(sharedQueue, "Consumer-Queue-Thread");
            consumerThread.setPriority(Thread.NORM_PRIORITY);
            consumerThread.start();

            poolingThread = new Thread(() -> {
                KafkaConsumer<String, GenericRecord> consumer = null;
                try {
                    consumer = new KafkaConsumer<>(kafkaProperties);
                    final TopicPartition partition = new TopicPartition(topic, 0);
                    consumer.assign(Arrays.asList(partition));
                    ConsumerRecords<String, GenericRecord> records = consumer.poll(1000);
                    LOG.info("waiting for records");
                    while (isRunning) {
                        if (records.count() >= 1) {
                            records.forEach(r -> {
                                queue.add(r.value());
                            });
                        }
                        records = consumer.poll(100);
                    }
                } catch (final Exception e) {
                    LOG.error("Consumer thread {} " + Thread.currentThread().getName(), e);
                } finally {
                    if (consumer != null) {
                        consumer.close();
                    }
                }
            }, "Consumer-Polling-Thread");
            poolingThread.setPriority(Thread.NORM_PRIORITY);
            poolingThread.start();

        }
    }

    /**
     * Stop consumer.
     */
    public void stop() {
        if (isRunning) {
            consumerThread.interrupt();
            poolingThread.interrupt();
            isRunning = false;
        }
    }

    /**
     * The {@link QueueConsumer} implements the queue which is shared among the producer and consumer.
     */
    static class QueueConsumer implements Runnable {
        private final static Logger LOG = LoggerFactory.getLogger(QueueConsumer.class);
        /**
         * Count the number of event consumer.
         */
        private int count = 0;

        /**
         * Map keeps the generic writer for respective events.
         */
        Map<String, GenericRecordCsvWriter> map = new HashMap<>();

        protected BlockingQueue<GenericRecord> queue = null;
        File tmpDir;

        public QueueConsumer(final BlockingQueue<GenericRecord> queue, final File tmpDir) {
            this.queue = queue;
            this.tmpDir = tmpDir;
            LOG.info("Blocking QueueConsumer created...");
        }

        private void closeWriters() {
            for (final GenericRecordCsvWriter writer : map.values()) {
                writer.close();
            }
        }

        /**
         * @return the count
         */
        public int getConsumeCount() {
            return count;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    count++;
                    write(queue.take());
                    LOG.trace("Message Consume  ={} ", count);
                }
            } catch (final InterruptedException | IOException e) {
                LOG.error("Error will consuming records", e);
            } finally {
                closeWriters();
            }
        }

        /**
         * Helper method to write generic records to respective event files.
         *
         * @param record
         *            Generic Record
         * @throws IOException
         */
        private void write(final GenericRecord record) throws IOException {
            GenericRecordCsvWriter genericRecordCsvWriter = map.get(record.getSchema().getFullName());
            if (genericRecordCsvWriter == null) {
                genericRecordCsvWriter = new GenericRecordCsvWriter(record.getSchema().getFullName(), tmpDir, false);
                map.put(record.getSchema().getFullName(), genericRecordCsvWriter);
            }
            genericRecordCsvWriter.write(record);
        }
    }
}
