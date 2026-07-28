package com.vbank.account_service.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbank.account_service.dto.LogMessage;
import com.vbank.account_service.dto.MessageType;
import com.vbank.account_service.exception.ApiError;
import com.vbank.account_service.exception.InvalidAppNameException;
import com.vbank.account_service.model.RequestContext;
import com.vbank.account_service.service.LoggingProducerService;
import com.vbank.account_service.util.SensitiveDataRedactor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestResponseLoggingFilter
        extends OncePerRequestFilter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    RequestResponseLoggingFilter.class
            );

    private static final String SERVICE_NAME = "account-service";

    private final LoggingProducerService loggingProducerService;
    private final SensitiveDataRedactor sensitiveDataRedactor;
    private final ObjectMapper objectMapper;
    private final int maximumBodyLength;

    public RequestResponseLoggingFilter(
            LoggingProducerService loggingProducerService,
            SensitiveDataRedactor sensitiveDataRedactor,
            ObjectMapper objectMapper,
            @Value("${vbank.logging.max-body-length:10000}")
            int maximumBodyLength
    ) {
        this.loggingProducerService = loggingProducerService;
        this.sensitiveDataRedactor = sensitiveDataRedactor;
        this.objectMapper = objectMapper;
        this.maximumBodyLength = maximumBodyLength;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        UUID correlationId =
                RequestContext.resolveOrCreateCorrelationId(
                        request.getHeader(
                                RequestContext.CORRELATION_ID_HEADER
                        )
                );

        request.setAttribute(
                RequestContext.CORRELATION_ID_ATTRIBUTE,
                correlationId
        );

        response.setHeader(
                RequestContext.CORRELATION_ID_HEADER,
                correlationId.toString()
        );

        ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request);

        ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);

        String appNameForLogging =
                RequestContext.appNameForLogging(
                        request.getHeader(
                                RequestContext.APP_NAME_HEADER
                        )
                );

        Instant requestTime = Instant.now();

        try {
            try {
                RequestContext validatedContext =
                        RequestContext.from(requestWrapper);

                appNameForLogging = validatedContext.appName();

                filterChain.doFilter(
                        requestWrapper,
                        responseWrapper
                );
            } catch (InvalidAppNameException exception) {
                writeInvalidAppNameResponse(
                        requestWrapper,
                        responseWrapper,
                        exception
                );
            }
        } finally {
            try {
                publishRequestLog(
                        requestWrapper,
                        correlationId,
                        appNameForLogging,
                        requestTime
                );

                publishResponseLog(
                        requestWrapper,
                        responseWrapper,
                        correlationId,
                        appNameForLogging
                );
            } catch (RuntimeException loggingException) {
                LOGGER.error(
                        "Could not prepare HTTP logs for correlation ID {}.",
                        correlationId,
                        loggingException
                );
            } finally {
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private void writeInvalidAppNameResponse(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            InvalidAppNameException exception
    ) throws IOException {

        response.resetBuffer();
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiError errorResponse = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );

        byte[] responseBody =
                objectMapper.writeValueAsBytes(errorResponse);

        response.getOutputStream().write(responseBody);
    }

    private void publishRequestLog(
            ContentCachingRequestWrapper request,
            UUID correlationId,
            String appName,
            Instant requestTime
    ) {
        loggingProducerService.send(
                LogMessage.builder()
                        .message(readRequestBody(request))
                        .messageType(MessageType.REQUEST)
                        .dateTime(requestTime)
                        .serviceName(SERVICE_NAME)
                        .httpMethod(request.getMethod())
                        .path(request.getRequestURI())
                        .statusCode(null)
                        .correlationId(correlationId)
                        .appName(appName)
                        .build()
        );
    }

    private void publishResponseLog(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            UUID correlationId,
            String appName
    ) {
        loggingProducerService.send(
                LogMessage.builder()
                        .message(readResponseBody(response))
                        .messageType(MessageType.RESPONSE)
                        .dateTime(Instant.now())
                        .serviceName(SERVICE_NAME)
                        .httpMethod(request.getMethod())
                        .path(request.getRequestURI())
                        .statusCode(response.getStatus())
                        .correlationId(correlationId)
                        .appName(appName)
                        .build()
        );
    }

    private String readRequestBody(
            ContentCachingRequestWrapper request
    ) {
        return sensitiveDataRedactor.redact(
                decode(
                        request.getContentAsByteArray(),
                        request.getCharacterEncoding()
                ),
                maximumBodyLength
        );
    }

    private String readResponseBody(
            ContentCachingResponseWrapper response
    ) {
        return sensitiveDataRedactor.redact(
                decode(
                        response.getContentAsByteArray(),
                        response.getCharacterEncoding()
                ),
                maximumBodyLength
        );
    }

    private String decode(
            byte[] content,
            String characterEncoding
    ) {
        if (content.length == 0) {
            return "{}";
        }

        Charset charset = StandardCharsets.UTF_8;

        if (characterEncoding != null
                && Charset.isSupported(characterEncoding)) {
            charset = Charset.forName(characterEncoding);
        }

        return new String(content, charset);
    }
}
