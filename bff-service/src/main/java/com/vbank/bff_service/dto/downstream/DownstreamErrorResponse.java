package com.vbank.bff_service.dto.downstream;

import java.time.Instant;

public record DownstreamErrorResponse(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path
) {
}
