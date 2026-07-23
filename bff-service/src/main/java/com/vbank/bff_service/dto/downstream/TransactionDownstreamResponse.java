package com.vbank.bff_service.dto.downstream;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDownstreamResponse(
        UUID transactionId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String status,
        String failureReason,
        Instant createdAt,
        Instant executedAt
) {
}
