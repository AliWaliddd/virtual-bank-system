package com.vbank.bff_service.exception;

public class DownstreamTimeoutException extends RuntimeException {

    private final String serviceName;

    public DownstreamTimeoutException(String serviceName) {
        super(serviceName + " did not respond within the configured timeout.");
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
