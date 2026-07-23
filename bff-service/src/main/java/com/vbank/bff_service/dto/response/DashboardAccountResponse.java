package com.vbank.bff_service.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DashboardAccountResponse(
        UUID accountId,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        String status,
        List<DashboardTransactionResponse> transactions
) {
}
