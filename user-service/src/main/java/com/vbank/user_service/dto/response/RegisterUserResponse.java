package com.vbank.user_service.dto.response;

import java.util.UUID;

public record RegisterUserResponse(
        UUID userId,
        String username,
        String message
) {
}