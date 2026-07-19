package com.vbank.account_service.exception;

public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException(String message) {
        super(message);
    }
}
