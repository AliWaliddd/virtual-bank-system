package com.vbank.bff_service.dto.downstream;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDownstreamResponse(
        UUID accountId,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        String status
) {
}
