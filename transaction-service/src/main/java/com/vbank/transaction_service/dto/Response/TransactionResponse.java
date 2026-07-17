package com.vbank.transaction_service.dto;

import com.vbank.transaction_service.enums.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID transactionId;

    private UUID fromAccountId;

    private UUID toAccountId;

    private BigDecimal amount;

    private TransactionStatus status;

    private String failureReason;

    private Instant createdAt;

    private Instant executedAt;
}