package com.vbank.user_service.config;

import com.vbank.user_service.dto.LogMessage;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private final String bootstrapServers;
    private final int maxBlockMilliseconds;
    private final int requestTimeoutMilliseconds;
    private final int deliveryTimeoutMilliseconds;

    public KafkaProducerConfig(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers,
            @Value("${vbank.logging.kafka.max-block-ms:1000}")
            int maxBlockMilliseconds,
            @Value("${vbank.logging.kafka.request-timeout-ms:3000}")
            int requestTimeoutMilliseconds,
            @Value("${vbank.logging.kafka.delivery-timeout-ms:5000}")
            int deliveryTimeoutMilliseconds
    ) {
        this.bootstrapServers = bootstrapServers;
        this.maxBlockMilliseconds = maxBlockMilliseconds;
        this.requestTimeoutMilliseconds = requestTimeoutMilliseconds;
        this.deliveryTimeoutMilliseconds = deliveryTimeoutMilliseconds;
    }

    @Bean
    public ProducerFactory<String, LogMessage> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );
        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        config.put(
                ProducerConfig.MAX_BLOCK_MS_CONFIG,
                maxBlockMilliseconds
        );
        config.put(
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                requestTimeoutMilliseconds
        );
        config.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                deliveryTimeoutMilliseconds
        );
        config.put(ProducerConfig.ACKS_CONFIG, "1");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, LogMessage> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
