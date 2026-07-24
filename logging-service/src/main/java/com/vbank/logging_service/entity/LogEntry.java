package com.vbank.logging_service.entity;

import com.vbank.logging_service.dto.MessageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private MessageType messageType;

    @Column(nullable = false)
    private Instant dateTime;

    @Column(nullable = false)
    private String serviceName;

    private String httpMethod;

    private String path;

    private Integer statusCode;

    private UUID correlationId;

    private String appName;
}