package com.vbank.user_service.dto;



import lombok.*;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LogMessage {


    private String message;
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    private Instant dateTime;

    private String serviceName;

    private String httpMethod;

    private String path;

    private Integer statusCode;

    private UUID correlationId;

    private String appName;
}
