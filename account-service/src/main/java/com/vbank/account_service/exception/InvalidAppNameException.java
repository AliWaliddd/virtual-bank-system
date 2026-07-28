package com.vbank.account_service.exception;

public class InvalidAppNameException extends RuntimeException {

    public InvalidAppNameException(String message) {
        super(message);
    }
}
