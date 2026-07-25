package com.vbank.transaction_service.dto.Response;

import java.time.Instant;

public record DownstreamErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}