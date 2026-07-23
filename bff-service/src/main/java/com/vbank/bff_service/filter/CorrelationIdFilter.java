package com.vbank.bff_service.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String ATTRIBUTE_NAME =
            CorrelationIdFilter.class.getName() + ".correlationId";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        String correlationId = resolveOrCreate(
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HEADER_NAME)
        );

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(
                        HEADER_NAME,
                        correlationId
                ))
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request)
                .build();

        mutatedExchange.getAttributes().put(
                ATTRIBUTE_NAME,
                correlationId
        );
        mutatedExchange.getResponse()
                .getHeaders()
                .set(HEADER_NAME, correlationId);

        return chain.filter(mutatedExchange);
    }

    public static String getCorrelationId(
            ServerWebExchange exchange
    ) {
        Object value = exchange.getAttribute(ATTRIBUTE_NAME);

        if (value instanceof String correlationId
                && !correlationId.isBlank()) {
            return correlationId;
        }

        String headerValue = exchange.getRequest()
                .getHeaders()
                .getFirst(HEADER_NAME);

        return resolveOrCreate(headerValue);
    }

    private static String resolveOrCreate(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return value.trim();
    }
}
