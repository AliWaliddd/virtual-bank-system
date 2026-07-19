package com.vbank.account_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(

        @NotNull(message = "User ID is required.")
        UUID userId,

        @NotBlank(message = "Account type is required.")
        @Pattern(
                regexp = "^\\s*(?i:SAVINGS|CHECKING)\\s*$",
                message = "Account type must be SAVINGS or CHECKING."
        )
        String accountType,

        @NotNull(message = "Initial balance is required.")
        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "Initial balance must be zero or greater."
        )
        @DecimalMax(
                value = "99999999999999999.99",
                inclusive = true,
                message = "Initial balance is too large."
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Initial balance may contain at most 17 integer digits and 2 decimal places."
        )
        BigDecimal initialBalance
) {
}
