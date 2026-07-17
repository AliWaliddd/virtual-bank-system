package com.vbank.user_service.dto.response;

import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String username
) {
}