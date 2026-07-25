package com.vbank.transaction_service.exceptions;

/**
 * Represents a temporary conflict in Account Service, such as
 * concurrent updates or an inability to acquire an account lock.
 */
public class AccountTransferConflictException extends RuntimeException {

    public AccountTransferConflictException(String message) {
        super(message);
    }
}