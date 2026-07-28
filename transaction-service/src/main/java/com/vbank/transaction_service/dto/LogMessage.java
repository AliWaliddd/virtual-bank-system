package com.vbank.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogMessage {

    private String message;
    private MessageType messageType;
    private Instant dateTime;
    private String serviceName;
    private String httpMethod;
    private String path;
    private Integer statusCode;
    private UUID correlationId;
    private String appName;
}
