package com.vbank.user_service.exception;

public class MissingAuthorizationHeaderException extends RuntimeException {

    public MissingAuthorizationHeaderException(String message) {
        super(message);
    }
}