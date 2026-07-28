package com.vbank.transaction_service.service.impl;

import com.vbank.transaction_service.dto.LogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoggingProducerService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LoggingProducerService.class);

    private final KafkaTemplate<String, LogMessage> kafkaTemplate;
    private final String topic;

    public LoggingProducerService(
            KafkaTemplate<String, LogMessage> kafkaTemplate,
            @Value("${vbank.logging.topic:vbank.logs}")
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void send(LogMessage logMessage) {
        try {
            String key = logMessage.getCorrelationId() == null
                    ? null
                    : logMessage.getCorrelationId().toString();

            kafkaTemplate.send(topic, key, logMessage)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            LOGGER.error(
                                    "Kafka failed to deliver {} log for correlation ID {}.",
                                    logMessage.getMessageType(),
                                    logMessage.getCorrelationId(),
                                    exception
                            );
                            return;
                        }

                        LOGGER.debug(
                                "Kafka delivered {} log for correlation ID {} to topic {}.",
                                logMessage.getMessageType(),
                                logMessage.getCorrelationId(),
                                topic
                        );
                    });
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Could not submit {} log for correlation ID {} to Kafka.",
                    logMessage.getMessageType(),
                    logMessage.getCorrelationId(),
                    exception
            );
        }
    }
}
