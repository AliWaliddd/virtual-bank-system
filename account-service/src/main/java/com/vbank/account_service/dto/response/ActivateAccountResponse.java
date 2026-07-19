package com.vbank.account_service.dto.response;

import com.vbank.account_service.entity.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record ActivateAccountResponse(
        UUID accountId,
        AccountStatus status,
        Instant reactivatedAt,
        String message
) {
}