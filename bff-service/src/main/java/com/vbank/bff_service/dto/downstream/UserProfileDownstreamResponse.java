package com.vbank.bff_service.dto.downstream;

import java.util.UUID;

public record UserProfileDownstreamResponse(
        UUID userId,
        String username,
        String email,
        String firstName,
        String lastName
) {
}
