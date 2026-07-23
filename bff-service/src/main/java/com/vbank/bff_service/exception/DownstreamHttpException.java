package com.vbank.bff_service.exception;

import org.springframework.http.HttpStatus;

public class DownstreamHttpException extends RuntimeException {

    private final String serviceName;
    private final int downstreamStatus;
    private final HttpStatus responseStatus;
    private final String downstreamMessage;

    public DownstreamHttpException(
            String serviceName,
            int downstreamStatus,
            HttpStatus responseStatus,
            String downstreamMessage
    ) {
        super(buildMessage(
                serviceName,
                downstreamStatus,
                downstreamMessage
        ));
        this.serviceName = serviceName;
        this.downstreamStatus = downstreamStatus;
        this.responseStatus = responseStatus;
        this.downstreamMessage = downstreamMessage;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getDownstreamStatus() {
        return downstreamStatus;
    }

    public HttpStatus getResponseStatus() {
        return responseStatus;
    }

    public String getDownstreamMessage() {
        return downstreamMessage;
    }

    private static String buildMessage(
            String serviceName,
            int status,
            String downstreamMessage
    ) {
        String detail = downstreamMessage == null
                || downstreamMessage.isBlank()
                ? "No error detail was provided."
                : downstreamMessage;

        return serviceName + " returned HTTP " + status
                + ": " + detail;
    }
}
