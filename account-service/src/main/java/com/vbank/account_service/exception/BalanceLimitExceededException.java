package com.vbank.account_service.exception;

public class BalanceLimitExceededException extends RuntimeException {

    public BalanceLimitExceededException(String message) {
        super(message);
    }
}
