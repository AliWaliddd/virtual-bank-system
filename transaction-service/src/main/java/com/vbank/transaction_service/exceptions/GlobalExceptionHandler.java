package com.vbank.transaction_service.exceptions;

import com.vbank.transaction_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*
     * ============================================================
     * Transaction business exceptions
     * ============================================================
     */

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(
            TransactionNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransfer(
            InvalidTransferException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(TransactionAlreadyProcessedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyProcessed(
            TransactionAlreadyProcessedException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidAppNameException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAppName(
            InvalidAppNameException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    /*
     * ============================================================
     * Request-body validation
     *
     * Handles @Valid failures such as:
     * - missing transactionId
     * - missing fromAccountId
     * - missing toAccountId
     * - zero or negative amount
     * ============================================================
     */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "One or more request fields are invalid.";
        }

        return buildError(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );
    }

    /*
     * Handles validation directly applied to controller method
     * parameters.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "One or more request values are invalid.",
                request.getRequestURI()
        );
    }

    /*
     * Handles Jakarta Bean Validation constraint failures outside
     * normal @RequestBody validation.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(violation ->
                        violation.getPropertyPath()
                                + ": "
                                + violation.getMessage()
                )
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "One or more request values are invalid.";
        }

        return buildError(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );
    }

    /*
     * Handles malformed path variables, including:
     *
     * GET /accounts/not-a-uuid/transactions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message;

        if (exception.getRequiredType() != null
                && exception.getRequiredType().equals(java.util.UUID.class)) {

            message = "Invalid UUID value for '"
                    + exception.getName()
                    + "'.";
        } else {
            message = "Invalid value for '"
                    + exception.getName()
                    + "'.";
        }

        return buildError(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );
    }

    /*
     * Handles:
     * - malformed JSON
     * - invalid UUID values in JSON
     * - missing request body
     * - unknown JSON fields
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        Throwable cause = exception.getMostSpecificCause();

        if (cause instanceof UnrecognizedPropertyException propertyException) {
            return buildError(
                    HttpStatus.BAD_REQUEST,
                    "Unknown JSON field '"
                            + propertyException.getPropertyName()
                            + "'.",
                    request.getRequestURI()
            );
        }

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Malformed or missing JSON request body.",
                request.getRequestURI()
        );
    }

    /*
     * ============================================================
     * HTTP protocol errors
     * ============================================================
     */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        String message =
                "HTTP method '"
                        + exception.getMethod()
                        + "' is not supported for this endpoint.";

        if (exception.getSupportedHttpMethods() != null
                && !exception.getSupportedHttpMethods().isEmpty()) {

            message += " Supported methods: "
                    + exception.getSupportedHttpMethods()
                    + ".";
        }

        return buildError(
                HttpStatus.METHOD_NOT_ALLOWED,
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        String receivedContentType =
                exception.getContentType() == null
                        ? "unknown"
                        : exception.getContentType().toString();

        String supportedContentTypes =
                exception.getSupportedMediaTypes().isEmpty()
                        ? "application/json"
                        : exception.getSupportedMediaTypes().toString();

        String message =
                "Content type '"
                        + receivedContentType
                        + "' is not supported. Supported content types: "
                        + supportedContentTypes
                        + ".";

        return buildError(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_ACCEPTABLE,
                "The requested response format is not supported.",
                request.getRequestURI()
        );
    }

    /*
     * ============================================================
     * Unknown endpoints
     * ============================================================
     */

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "The requested endpoint was not found.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "The requested endpoint was not found.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccountTransferRejectedException.class)
    public ResponseEntity<ErrorResponse> handleAccountTransferRejected(
            AccountTransferRejectedException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccountTransferConflictException.class)
    public ResponseEntity<ErrorResponse> handleAccountTransferConflict(
            AccountTransferConflictException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccountServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAccountServiceUnavailable(
            AccountServiceUnavailableException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccountServiceBadGatewayException.class)
    public ResponseEntity<ErrorResponse> handleAccountServiceBadGateway(
            AccountServiceBadGatewayException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_GATEWAY,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    /*
     * ============================================================
     * Final fallback
     * ============================================================
     */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request
    ) {
        /*
         * Log the complete exception internally, but do not expose
         * its message or stack trace to the API caller.
         */
        LOGGER.error(
                "Unexpected error while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred.",
                request.getRequestURI()
        );
    }

    private String formatFieldError(FieldError fieldError) {
        String defaultMessage = fieldError.getDefaultMessage();

        if (defaultMessage == null || defaultMessage.isBlank()) {
            defaultMessage = "Invalid value";
        }

        return fieldError.getField() + ": " + defaultMessage;
    }

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String message,
            String path
    ) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
