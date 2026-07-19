package com.vbank.account_service.dto.response;

import com.vbank.account_service.entity.AccountStatus;
import com.vbank.account_service.entity.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        String accountNumber,
        AccountType accountType,
        BigDecimal balance,
        AccountStatus status
) {
}
