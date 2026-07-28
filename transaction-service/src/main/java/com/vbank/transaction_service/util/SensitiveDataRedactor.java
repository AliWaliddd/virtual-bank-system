package com.vbank.transaction_service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class SensitiveDataRedactor {

    private static final String REDACTED = "[REDACTED]";
    private static final String UNREADABLE_BODY =
            "[BODY OMITTED: invalid or non-JSON content]";

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password",
            "passwordhash",
            "authorization",
            "accesstoken",
            "refreshtoken",
            "token",
            "apikey",
            "api-key"
    );

    private final ObjectMapper objectMapper;

    public SensitiveDataRedactor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String redact(String body, int maximumLength) {
        if (body == null || body.isBlank()) {
            return "{}";
        }

        try {
            JsonNode root = objectMapper.readTree(body);

            if (root == null) {
                return "{}";
            }

            redactNode(root);
            return truncate(
                    objectMapper.writeValueAsString(root),
                    maximumLength
            );
        } catch (JsonProcessingException exception) {
            return UNREADABLE_BODY;
        }
    }

    private void redactNode(JsonNode node) {
        if (node == null) {
            return;
        }

        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields =
                    objectNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();

                if (isSensitive(field.getKey())) {
                    objectNode.put(field.getKey(), REDACTED);
                } else {
                    redactNode(field.getValue());
                }
            }
            return;
        }

        if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::redactNode);
        }
    }

    private boolean isSensitive(String fieldName) {
        String normalized = fieldName
                .replace("_", "")
                .replace("-", "")
                .toLowerCase(Locale.ROOT);

        return SENSITIVE_FIELDS.contains(normalized);
    }

    private String truncate(String value, int maximumLength) {
        int safeMaximumLength = Math.max(maximumLength, 1);

        if (value.length() <= safeMaximumLength) {
            return value;
        }

        return value.substring(0, safeMaximumLength)
                + "...[TRUNCATED]";
    }
}
