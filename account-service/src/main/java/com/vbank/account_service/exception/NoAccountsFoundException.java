package com.vbank.account_service.exception;

public class NoAccountsFoundException extends RuntimeException {

    public NoAccountsFoundException(String message) {
        super(message);
    }
}
