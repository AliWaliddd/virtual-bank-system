package com.vbank.transaction_service.dto.Request;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(

        @NotNull(message = "From account ID is required.")
        UUID fromAccountId,

        @NotNull(message = "To account ID is required.")
        UUID toAccountId,

        @NotNull(message = "Amount is required.")
        @DecimalMin(
                value = "0.01",
                inclusive = true,
                message = "Amount must be greater than zero."
        )
        @DecimalMax(
                value = "99999999999999999.99",
                inclusive = true,
                message = "Amount is too large."
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Amount may contain at most 17 integer digits and 2 decimal places."
        )
        BigDecimal amount
) {
}

