package com.vbank.transaction_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferExecutionRequest {

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;
}