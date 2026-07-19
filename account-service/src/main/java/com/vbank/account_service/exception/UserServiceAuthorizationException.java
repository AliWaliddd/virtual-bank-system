package com.vbank.account_service.exception;

public class UserServiceAuthorizationException extends RuntimeException {

    public UserServiceAuthorizationException(String message) {
        super(message);
    }
}
