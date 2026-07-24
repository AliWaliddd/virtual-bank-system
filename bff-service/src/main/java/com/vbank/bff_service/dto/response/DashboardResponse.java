package com.vbank.bff_service.dto.response;

import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        UUID userId,
        String username,
        String email,
        String firstName,
        String lastName,
        List<DashboardAccountResponse> accounts
) {
}
