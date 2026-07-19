package com.vbank.account_service.dto.response;

import java.util.UUID;

public record CreateAccountResponse(
        UUID accountId,
        String accountNumber,
        String message
) {
}
