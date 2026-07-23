package com.vbank.bff_service.model;

import org.springframework.http.HttpHeaders;

public record RequestContext(
        String authorization,
        String appName,
        String correlationId
) {

    public void applyTo(HttpHeaders headers) {
        if (authorization != null && !authorization.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }

        if (appName != null && !appName.isBlank()) {
            headers.set("APP-NAME", appName);
        }

        headers.set("X-Correlation-ID", correlationId);
    }
}
