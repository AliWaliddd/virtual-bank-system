package com.vbank.account_service.service;

import com.vbank.account_service.dto.LogMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoggingProducerService {

    private static final String TOPIC = "vbank.logs";

    private final KafkaTemplate<String, LogMessage> kafkaTemplate;

    public void send(LogMessage logMessage) {
        kafkaTemplate.send(TOPIC, logMessage);
    }
}
