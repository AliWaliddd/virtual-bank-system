package com.vbank.bff_service.client;

import com.vbank.bff_service.dto.downstream.DownstreamErrorResponse;
import com.vbank.bff_service.exception.DownstreamHttpException;
import com.vbank.bff_service.exception.DownstreamServiceUnavailableException;
import com.vbank.bff_service.exception.DownstreamTimeoutException;
import com.vbank.bff_service.exception.MalformedDownstreamResponseException;
import com.vbank.bff_service.model.RequestContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;

public abstract class DownstreamClientSupport {

    private final Duration requestTimeout;

    protected DownstreamClientSupport(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    protected <T> Mono<T> retrieve(
            String serviceName,
            WebClient.RequestHeadersSpec<?> request,
            Class<T> bodyType,
            RequestContext requestContext
    ) {
        return execute(
                serviceName,
                request,
                requestContext,
                response -> response.bodyToMono(bodyType)
        );
    }

    protected <T> Mono<T> retrieve(
            String serviceName,
            WebClient.RequestHeadersSpec<?> request,
            ParameterizedTypeReference<T> bodyType,
            RequestContext requestContext
    ) {
        return execute(
                serviceName,
                request,
                requestContext,
                response -> response.bodyToMono(bodyType)
        );
    }

    private <T> Mono<T> execute(
            String serviceName,
            WebClient.RequestHeadersSpec<?> request,
            RequestContext requestContext,
            SuccessBodyDecoder<T> bodyDecoder
    ) {
        return request
                .headers(requestContext::applyTo)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return bodyDecoder.decode(response)
                                .switchIfEmpty(Mono.error(
                                        new MalformedDownstreamResponseException(
                                                serviceName,
                                                "The response body was empty."
                                        )
                                ));
                    }

                    return toHttpException(serviceName, response);
                })
                .timeout(
                        requestTimeout,
                        Mono.error(new DownstreamTimeoutException(
                                serviceName
                        ))
                )
                .onErrorMap(
                        WebClientRequestException.class,
                        exception -> new DownstreamServiceUnavailableException(
                                serviceName,
                                exception
                        )
                )
                .onErrorMap(
                        CodecException.class,
                        exception -> new MalformedDownstreamResponseException(
                                serviceName,
                                "The JSON body could not be decoded.",
                                exception
                        )
                )
                .onErrorMap(
                        DataBufferLimitException.class,
                        exception -> new MalformedDownstreamResponseException(
                                serviceName,
                                "The response body exceeded the configured size limit.",
                                exception
                        )
                );
    }

    private <T> Mono<T> toHttpException(
            String serviceName,
            ClientResponse response
    ) {
        HttpStatusCode statusCode = response.statusCode();

        return response.bodyToMono(DownstreamErrorResponse.class)
                .onErrorResume(ignored -> Mono.empty())
                .defaultIfEmpty(new DownstreamErrorResponse(
                        null,
                        statusCode.value(),
                        null,
                        null,
                        null
                ))
                .flatMap(error -> Mono.error(
                        new DownstreamHttpException(
                                serviceName,
                                statusCode.value(),
                                mapPublicStatus(statusCode),
                                publicMessage(
                                        serviceName,
                                        statusCode,
                                        error.message()
                                )
                        )
                ));
    }

    private HttpStatus mapPublicStatus(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    private String publicMessage(
            String serviceName,
            HttpStatusCode statusCode,
            String downstreamMessage
    ) {
        if (statusCode.is5xxServerError()) {
            return serviceName
                    + " failed while processing the dashboard request.";
        }

        if (downstreamMessage == null
                || downstreamMessage.isBlank()) {
            return "The downstream service did not provide an error message.";
        }

        return downstreamMessage.trim();
    }

    @FunctionalInterface
    private interface SuccessBodyDecoder<T> {
        Mono<T> decode(ClientResponse response);
    }
}
