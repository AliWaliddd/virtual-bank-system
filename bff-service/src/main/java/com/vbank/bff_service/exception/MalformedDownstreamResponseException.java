package com.vbank.bff_service.exception;

public class MalformedDownstreamResponseException
        extends RuntimeException {

    private final String serviceName;

    public MalformedDownstreamResponseException(
            String serviceName,
            String detail
    ) {
        super(serviceName + " returned a malformed response. " + detail);
        this.serviceName = serviceName;
    }

    public MalformedDownstreamResponseException(
            String serviceName,
            String detail,
            Throwable cause
    ) {
        super(serviceName + " returned a malformed response. " + detail, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
