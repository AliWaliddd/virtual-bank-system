package com.vbank.transaction_service.exceptions;

public class InvalidAppNameException extends RuntimeException {

    public InvalidAppNameException(String message) {
        super(message);
    }
}
