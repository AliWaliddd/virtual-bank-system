package com.vbank.transaction_service.exceptions;

public class AccountServiceBadGatewayException extends RuntimeException {

    public AccountServiceBadGatewayException(String message) {
        super(message);
    }
}