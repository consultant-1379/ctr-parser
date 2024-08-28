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

package com.ericsson.oss.adc.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.ericsson.oss.adc.handler.data_catalog.DataCatalogHandler;

@Configuration
@Component
public class KafkaConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConfiguration.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServerConfig;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupIdConsumerConfig;

    @Value("${spring.kafka.topics.output.compression-type}")
    private String compressionTypeConfig;

    @Value("${spring.kafka.topics.output.batch-size}")
    private String batchSizeConfig;

    @Value("${spring.kafka.topics.output.buffer-memory}")
    private String bufferMemoryConfig;

    @Value("${spring.kafka.topics.output.max-request-size}")
    private String maxRequestSizeConfig;

    @Value("${spring.kafka.topics.output.linger}")
    private String lingerConfig;

    @Value("${spring.kafka.topics.output.acks}")
    private String acksConfig;

    @Value("${spring.kafka.topics.input.session.timeout.ms}")
    private int sessionTimeoutMs;

    @Value("${spring.kafka.topics.input.partition.assignment.strategy}")
    private String partitionAssignmentStrategy;

    //DMM Config
    @Value("${dmm.data-catalog.data-provider-type}")
    private String dataProviderType;

    @Value("${dmm.data-catalog.data-space}")
    private String dataSpace;

    @Value("${dmm.data-catalog.data-category}")
    private String dataCategory;

    @Autowired
    private DataCatalogHandler dataCatalogHandler;

    @Autowired
    private Environment environment;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        final Map<String, Object> adminConfig = new HashMap<>();
        adminConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
        return new KafkaAdmin(adminConfig);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> consumerKafkaListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerInputFactory(bootstrapServerConfig));

        factory.getContainerProperties().setEosMode(ContainerProperties.EOSMode.BETA);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> consumerInputFactory(final String bootstrapServerConsumerConfig) {

        final Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConsumerConfig);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupIdConsumerConfig);
        config.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, partitionAssignmentStrategy);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Message<byte[]>> kafkaOutputTemplate() {
        return new KafkaTemplate<>(producerOutputFactory());
    }

    @Bean
    public ProducerFactory<String, Message<byte[]>> producerOutputFactory() {
        final Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionTypeConfig);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSizeConfig);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemoryConfig);
        config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSizeConfig);
        config.put(ProducerConfig.LINGER_MS_CONFIG, lingerConfig);
        config.put(ProducerConfig.ACKS_CONFIG, acksConfig);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Make a query to Data Catalog to fetch the Kafka Access Endpoints
     *
     * @return list containing bootstrap server config
     */
    protected String getKafkaAccessPoints() {
        LOG.info("Returning default bootstrap server: {}", bootstrapServerConfig);
        return bootstrapServerConfig;
    }
}
