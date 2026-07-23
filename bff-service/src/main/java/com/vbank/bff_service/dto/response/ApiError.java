package com.vbank.bff_service.dto.response;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        String downstreamService,
        Integer downstreamStatus
) {
}
