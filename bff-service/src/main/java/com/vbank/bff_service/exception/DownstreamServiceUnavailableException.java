package com.vbank.bff_service.exception;

public class DownstreamServiceUnavailableException
        extends RuntimeException {

    private final String serviceName;

    public DownstreamServiceUnavailableException(
            String serviceName,
            Throwable cause
    ) {
        super(serviceName + " is currently unavailable.", cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
