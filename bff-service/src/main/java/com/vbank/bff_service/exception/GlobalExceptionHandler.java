package com.vbank.bff_service.exception;

import com.vbank.bff_service.dto.response.ApiError;
import com.vbank.bff_service.filter.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GlobalExceptionHandler.class
    );

    @ExceptionHandler(InvalidUuidException.class)
    public ResponseEntity<ApiError> handleInvalidUuid(
            InvalidUuidException exception,
            ServerWebExchange exchange
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                exchange,
                null,
                null
        );
    }

    @ExceptionHandler(MissingAuthorizationHeaderException.class)
    public ResponseEntity<ApiError> handleMissingAuthorization(
            MissingAuthorizationHeaderException exception,
            ServerWebExchange exchange
    ) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                exchange,
                null,
                null
        );
    }

    @ExceptionHandler(DownstreamHttpException.class)
    public ResponseEntity<ApiError> handleDownstreamHttpError(
            DownstreamHttpException exception,
            ServerWebExchange exchange
    ) {
        LOGGER.warn(
                "Downstream HTTP failure from {} with status {}. Correlation ID: {}",
                exception.getServiceName(),
                exception.getDownstreamStatus(),
                CorrelationIdFilter.getCorrelationId(exchange)
        );

        return buildError(
                exception.getResponseStatus(),
                exception.getDownstreamMessage(),
                exchange,
                exception.getServiceName(),
                exception.getDownstreamStatus()
        );
    }

    @ExceptionHandler(DownstreamServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleUnavailable(
            DownstreamServiceUnavailableException exception,
            ServerWebExchange exchange
    ) {
        LOGGER.warn(
                "Downstream service unavailable: {}. Correlation ID: {}",
                exception.getServiceName(),
                CorrelationIdFilter.getCorrelationId(exchange)
        );

        return buildError(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage(),
                exchange,
                exception.getServiceName(),
                null
        );
    }

    @ExceptionHandler(DownstreamTimeoutException.class)
    public ResponseEntity<ApiError> handleTimeout(
            DownstreamTimeoutException exception,
            ServerWebExchange exchange
    ) {
        LOGGER.warn(
                "Downstream timeout: {}. Correlation ID: {}",
                exception.getServiceName(),
                CorrelationIdFilter.getCorrelationId(exchange)
        );

        return buildError(
                HttpStatus.GATEWAY_TIMEOUT,
                exception.getMessage(),
                exchange,
                exception.getServiceName(),
                null
        );
    }

    @ExceptionHandler(MalformedDownstreamResponseException.class)
    public ResponseEntity<ApiError> handleMalformedDownstreamResponse(
            MalformedDownstreamResponseException exception,
            ServerWebExchange exchange
    ) {
        LOGGER.warn(
                "Malformed downstream response from {}. Correlation ID: {}",
                exception.getServiceName(),
                CorrelationIdFilter.getCorrelationId(exchange)
        );

        return buildError(
                HttpStatus.BAD_GATEWAY,
                exception.getMessage(),
                exchange,
                exception.getServiceName(),
                null
        );
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            ServerWebInputException exception,
            ServerWebExchange exchange
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "The request contains an invalid value.",
                exchange,
                null,
                null
        );
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(
            MethodNotAllowedException exception,
            ServerWebExchange exchange
    ) {
        return buildError(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method '"
                        + exception.getHttpMethod()
                        + "' is not supported for this endpoint.",
                exchange,
                null,
                null
        );
    }

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(
            UnsupportedMediaTypeStatusException exception,
            ServerWebExchange exchange
    ) {
        return buildError(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "The request content type is not supported.",
                exchange,
                null,
                null
        );
    }

    @ExceptionHandler(NotAcceptableStatusException.class)
    public ResponseEntity<ApiError> handleNotAcceptable(
            NotAcceptableStatusException exception,
            ServerWebExchange exchange
    ) {
        return buildError(
                HttpStatus.NOT_ACCEPTABLE,
                "The requested response format is not supported.",
                exchange,
                null,
                null
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException exception,
            ServerWebExchange exchange
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "The requested endpoint was not found.",
                exchange,
                null,
                null
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(
            ResponseStatusException exception,
            ServerWebExchange exchange
    ) {
        HttpStatus status = HttpStatus.resolve(
                exception.getStatusCode().value()
        );

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = exception.getReason();
        if (message == null || message.isBlank()) {
            message = status.getReasonPhrase();
        }

        return buildError(
                status,
                message,
                exchange,
                null,
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedError(
            Exception exception,
            ServerWebExchange exchange
    ) {
        LOGGER.error(
                "Unexpected BFF error. Correlation ID: {}",
                CorrelationIdFilter.getCorrelationId(exchange),
                exception
        );

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred.",
                exchange,
                null,
                null
        );
    }

    private ResponseEntity<ApiError> buildError(
            HttpStatus status,
            String message,
            ServerWebExchange exchange,
            String downstreamService,
            Integer downstreamStatus
    ) {
        String correlationId =
                CorrelationIdFilter.getCorrelationId(exchange);

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value(),
                correlationId,
                downstreamService,
                downstreamStatus
        );

        return ResponseEntity.status(status)
                .header(
                        CorrelationIdFilter.HEADER_NAME,
                        correlationId
                )
                .body(error);
    }
}
