package com.vbank.bff_service.model;

import org.springframework.http.HttpHeaders;

public record RequestContext(
        String appName,
        String correlationId
) {

    public void applyTo(HttpHeaders headers) {

        if (appName != null && !appName.isBlank()) {
            headers.set("APP-NAME", appName);
        }

        if (correlationId != null && !correlationId.isBlank()) {
            headers.set(
                    "X-Correlation-ID",
                    correlationId
            );
        }
    }
}
