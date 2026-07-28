package com.vbank.transaction_service.model;

import com.vbank.transaction_service.exceptions.InvalidAppNameException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record RequestContext(
        String appName,
        UUID correlationId
) {

    public static final String APP_NAME_HEADER = "APP-NAME";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_ATTRIBUTE =
            RequestContext.class.getName() + ".correlationId";

    private static final Set<String> ALLOWED_APP_NAMES =
            Set.of("PORTAL", "MOBILE");

    public static RequestContext from(HttpServletRequest request) {
        String appName = validateAppName(
                request.getHeader(APP_NAME_HEADER)
        );

        UUID correlationId = resolveCorrelationId(request);

        return new RequestContext(
                appName,
                correlationId
        );
    }

    public void applyTo(HttpHeaders headers) {
        headers.set(APP_NAME_HEADER, appName);
        headers.set(
                CORRELATION_ID_HEADER,
                correlationId.toString()
        );
    }

    public static UUID resolveOrCreateCorrelationId(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID();
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            return UUID.randomUUID();
        }
    }

    public static String appNameForLogging(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        return ALLOWED_APP_NAMES.contains(normalized)
                ? normalized
                : "UNKNOWN";
    }

    private static String validateAppName(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAppNameException(
                    "APP-NAME header is required. Allowed values are PORTAL and MOBILE."
            );
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_APP_NAMES.contains(normalized)) {
            throw new InvalidAppNameException(
                    "Invalid APP-NAME header. Allowed values are PORTAL and MOBILE."
            );
        }

        return normalized;
    }

    private static UUID resolveCorrelationId(
            HttpServletRequest request
    ) {
        Object attribute = request.getAttribute(
                CORRELATION_ID_ATTRIBUTE
        );

        if (attribute instanceof UUID correlationId) {
            return correlationId;
        }

        return resolveOrCreateCorrelationId(
                request.getHeader(CORRELATION_ID_HEADER)
        );
    }
}
