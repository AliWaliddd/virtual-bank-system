package com.vbank.transaction_service.dto.Response;



import com.vbank.transaction_service.entity.AccountStatus;
import com.vbank.transaction_service.entity.AccountType;

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