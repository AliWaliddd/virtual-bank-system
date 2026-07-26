package com.vbank.logging_service.service;


import com.vbank.logging_service.dto.LogMessage;
import com.vbank.logging_service.entity.LogEntry;
import com.vbank.logging_service.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;

    public void save(LogMessage message) {

        LogEntry logEntry = LogEntry.builder()
                .message(message.getMessage())
                .messageType(message.getMessageType())
                .dateTime(message.getDateTime())
                .serviceName(message.getServiceName())
                .httpMethod(message.getHttpMethod())
                .path(message.getPath())
                .statusCode(message.getStatusCode())
                .correlationId(message.getCorrelationId())
                .appName(message.getAppName())
                .build();

        logRepository.save(logEntry);
    }
}